package com.converty.app.core.conversion

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class PptxReadLimits(
    val maxEntries: Int = 4_096,
    val maxXmlBytes: Long = 4L * 1024 * 1024,
    val maxImageBytes: Long = 64L * 1024 * 1024,
    val maxTotalUncompressedBytes: Long = 256L * 1024 * 1024,
    val maxCompressionRatio: Long = 100,
)

data class PptxTransform(
    val bounds: EmuRect,
    val rotationDegrees: Float = 0f,
    val flipHorizontally: Boolean = false,
    val flipVertically: Boolean = false,
)

sealed interface PptxFill {
    data object None : PptxFill
    data class Solid(val argb: Int) : PptxFill
}

data class PptxLineStyle(
    val fill: PptxFill,
    val widthEmu: Long,
)

enum class PptxPresetGeometry { RECT, ROUND_RECT, ELLIPSE, LINE }

enum class PptxParagraphAlignment { LEFT, CENTER, RIGHT, JUSTIFY }

data class PptxTextRun(
    val text: String,
    val fontSizePoints: Float = 18f,
    val argb: Int = 0xFF000000.toInt(),
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontFamily: String = "sans-serif",
)

data class PptxTextParagraph(
    val runs: List<PptxTextRun>,
    val alignment: PptxParagraphAlignment = PptxParagraphAlignment.LEFT,
)

data class PptxTextFrame(
    val paragraphs: List<PptxTextParagraph>,
    val insetLeftEmu: Long = 91_440,
    val insetTopEmu: Long = 45_720,
    val insetRightEmu: Long = 91_440,
    val insetBottomEmu: Long = 45_720,
)

sealed interface PptxSlideElement {
    val transform: PptxTransform

    data class Picture(
        val imagePart: String,
        val imageMimeType: String,
        override val transform: PptxTransform,
    ) : PptxSlideElement

    data class Shape(
        val geometry: PptxPresetGeometry,
        val fill: PptxFill,
        val line: PptxLineStyle?,
        val textFrame: PptxTextFrame?,
        override val transform: PptxTransform,
    ) : PptxSlideElement
}

data class ParsedPptxSlide(
    val index: Int,
    /** PresentationML spTree child order; this is also the Canvas z-order. */
    val elements: List<PptxSlideElement>,
    val unsupportedFeatures: Set<String> = emptySet(),
) {
    private val firstPicture: PptxSlideElement.Picture?
        get() = elements.filterIsInstance<PptxSlideElement.Picture>().firstOrNull()

    /** Compatibility accessors kept for the app-generated single-image contract. */
    val imagePart: String?
        get() = firstPicture?.imagePart
    val imageMimeType: String?
        get() = firstPicture?.imageMimeType
    val placement: EmuRect?
        get() = firstPicture?.transform?.bounds

    val isSupported: Boolean
        get() = elements.isNotEmpty()
}

class ParsedPptx internal constructor(
    private val zip: ZipFile,
    val slideSize: EmuSize,
    val slides: List<ParsedPptxSlide>,
    private val limits: PptxReadLimits,
) : Closeable {
    fun readImage(slide: ParsedPptxSlide): ByteArray {
        val picture = requireNotNull(slide.elements.filterIsInstance<PptxSlideElement.Picture>().firstOrNull())
        return readImage(picture)
    }

    fun readImage(picture: PptxSlideElement.Picture): ByteArray {
        val path = picture.imagePart
        val entry = requireNotNull(zip.getEntry(path)) { "Missing image part: $path" }
        return zip.getInputStream(entry).use { it.readLimitedBytes(limits.maxImageBytes) }
    }

    override fun close() = zip.close()
}

enum class PptxParseFailureCode { MALFORMED, SECURITY_LIMIT, UNSUPPORTED_CONTAINER }

sealed interface PptxParseResult {
    data class Success(val presentation: ParsedPptx) : PptxParseResult
    data class Failure(
        val code: PptxParseFailureCode,
        val message: String,
        val cause: Throwable? = null,
    ) : PptxParseResult
}

/** Secure PresentationML reader for pictures and basic shapes/text; complex content becomes warnings. */
object PptxPackageParser {
    fun open(file: File, limits: PptxReadLimits = PptxReadLimits()): PptxParseResult {
        var zip: ZipFile? = null
        return try {
            zip = ZipFile(file)
            validateArchive(zip, limits)
            val presentationXml = zip.xml("ppt/presentation.xml", limits)
            val slideSize = presentationXml.singleElement(PML, "sldSz").let {
                EmuSize(it.longAttribute("cx"), it.longAttribute("cy"))
            }
            val presentationRels = relationships(zip, "ppt/_rels/presentation.xml.rels", limits)
            val slideRelationshipIds = presentationXml.elements(PML, "sldId").map {
                it.getAttributeNS(REL_OFFICE, "id").required("Slide relationship id")
            }
            if (slideRelationshipIds.isEmpty()) malformed("Presentation contains no slides")
            val slides = slideRelationshipIds.mapIndexed { index, relationshipId ->
                val relationship = presentationRels[relationshipId]
                    ?: malformed("Missing presentation relationship: $relationshipId")
                if (!relationship.type.endsWith("/slide") || relationship.external) {
                    return@mapIndexed ParsedPptxSlide(index, emptyList(), setOf("external-or-non-slide-relationship"))
                }
                parseSlide(zip, index, resolvePart("ppt/presentation.xml", relationship.target), limits)
            }
            PptxParseResult.Success(ParsedPptx(zip, slideSize, slides, limits)).also { zip = null }
        } catch (e: SecurityLimitException) {
            PptxParseResult.Failure(PptxParseFailureCode.SECURITY_LIMIT, e.message ?: "PPTX security limit exceeded", e)
        } catch (e: Exception) {
            PptxParseResult.Failure(PptxParseFailureCode.MALFORMED, e.message ?: "Malformed PPTX", e)
        } finally {
            zip?.close()
        }
    }

    private fun parseSlide(zip: ZipFile, index: Int, slidePath: String, limits: PptxReadLimits): ParsedPptxSlide {
        val document = zip.xml(slidePath, limits)
        val unsupported = linkedSetOf<String>()
        val shapeTree = document.singleElement(PML, "spTree")
        val slideRelationships = relationshipsIfPresent(zip, relationshipPartFor(slidePath), limits)
        val elements = mutableListOf<PptxSlideElement>()
        for (i in 0 until shapeTree.childNodes.length) {
            val node = shapeTree.childNodes.item(i)
            if (node !is Element || node.namespaceURI != PML) continue
            when (node.localName) {
                "nvGrpSpPr", "grpSpPr", "extLst" -> Unit
                "pic" -> parsePicture(zip, slidePath, node, slideRelationships, unsupported)?.let(elements::add)
                "sp" -> parseShape(node, unsupported)?.let(elements::add)
                else -> unsupported += when (node.localName) {
                    "graphicFrame" -> "chart-table-or-smartart"
                    "grpSp" -> "group-shape"
                    "cxnSp" -> "connector"
                    else -> "slide-element:${node.localName}"
                }
            }
        }
        if (document.elements(PML, "bg").isNotEmpty()) unsupported += "custom-background"
        if (elements.isEmpty()) unsupported += "no-renderable-content"
        return ParsedPptxSlide(index, elements, unsupported)
    }

    private fun parsePicture(
        zip: ZipFile,
        slidePath: String,
        picture: Element,
        slideRelationships: Map<String, Relationship>,
        warnings: MutableSet<String>,
    ): PptxSlideElement.Picture? {
        val blipFill = picture.directElement(PML, "blipFill")
        val blip = blipFill?.directElement(DML, "blip")
        val imageRelId = blip?.getAttributeNS(REL_OFFICE, "embed")?.takeIf { it.isNotBlank() }
        if (imageRelId == null) {
            warnings += "external-or-missing-image"
            return null
        }
        if (blipFill.directElements(DML, "tile").isNotEmpty()) warnings += "tiled-image"
        if (blipFill.directElements(DML, "srcRect").any { it.hasNonZeroCrop() }) warnings += "cropped-image"

        val shapeProperties = picture.directElement(PML, "spPr")
        if (shapeProperties?.directElement(DML, "ln") != null) warnings += "picture-border"
        if (shapeProperties?.directElement(DML, "effectLst") != null ||
            shapeProperties?.directElement(DML, "effectDag") != null
        ) {
            warnings += "picture-effect"
        }
        val transform = parseTransform(shapeProperties?.directElement(DML, "xfrm"), warnings) ?: return null
        val relationship = slideRelationships[imageRelId]
        if (relationship == null || relationship.external || !relationship.type.endsWith("/image")) {
            warnings += "external-or-missing-image"
            return null
        }
        val imagePath = resolvePart(slidePath, relationship.target)
        if (zip.getEntry(imagePath) == null) {
            warnings += "missing-image-part"
            return null
        }
        val mimeType = when (imagePath.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> null
        }
        if (mimeType == null) {
            warnings += "unsupported-image-format"
            return null
        }
        return PptxSlideElement.Picture(imagePath, mimeType, transform)
    }

    private fun parseTransform(
        transform: Element?,
        warnings: MutableSet<String>,
        allowSingleZeroExtent: Boolean = false,
    ): PptxTransform? {
        if (transform == null) {
            warnings += "missing-transform"
            return null
        }
        val off = transform.directElement(DML, "off")
        val ext = transform.directElement(DML, "ext")
        val bounds = try {
            if (off == null || ext == null) null else EmuRect(
                off.longAttribute("x"),
                off.longAttribute("y"),
                ext.longAttribute("cx"),
                ext.longAttribute("cy"),
            ).takeIf {
                if (allowSingleZeroExtent) {
                    it.width >= 0 && it.height >= 0 && (it.width > 0 || it.height > 0)
                } else {
                    it.width > 0 && it.height > 0
                }
            }
        } catch (_: Exception) {
            null
        }
        if (bounds == null) {
            warnings += "invalid-transform"
            return null
        }
        val rawRotation = transform.getAttribute("rot")
        val rotation = rawRotation.takeIf(String::isNotBlank)?.toLongOrNull()?.div(60_000f) ?: 0f
        if (rawRotation.isNotBlank() && rawRotation.toLongOrNull() == null) warnings += "invalid-rotation"
        return PptxTransform(
            bounds = bounds,
            rotationDegrees = rotation,
            flipHorizontally = transform.booleanAttribute("flipH", warnings),
            flipVertically = transform.booleanAttribute("flipV", warnings),
        )
    }

    private fun parseShape(shape: Element, warnings: MutableSet<String>): PptxSlideElement.Shape? {
        if (shape.elements(PML, "ph").isNotEmpty()) warnings += "placeholder-style-fallback"
        val hasInheritedStyle = shape.directElement(PML, "style") != null
        if (hasInheritedStyle) warnings += "theme-or-master-style-fallback"
        val shapeProperties = shape.directElement(PML, "spPr")
        if (shapeProperties == null) {
            warnings += "missing-shape-properties"
            return null
        }
        val presetName = shapeProperties.directElement(DML, "prstGeom")?.getAttribute("prst")
        val geometry = when (presetName) {
            "rect" -> PptxPresetGeometry.RECT
            "roundRect" -> PptxPresetGeometry.ROUND_RECT
            "ellipse" -> PptxPresetGeometry.ELLIPSE
            "line" -> PptxPresetGeometry.LINE
            else -> {
                warnings += if (presetName.isNullOrBlank()) {
                    "custom-or-missing-geometry-fallback"
                } else {
                    "preset-geometry-fallback:$presetName"
                }
                PptxPresetGeometry.RECT
            }
        }
        val transform = parseTransform(
            shapeProperties.directElement(DML, "xfrm"),
            warnings,
            allowSingleZeroExtent = geometry == PptxPresetGeometry.LINE,
        ) ?: return null
        if (shapeProperties.directElement(DML, "custGeom") != null) {
            warnings += "custom-geometry-fallback"
        }
        listOf("gradFill", "pattFill", "blipFill", "effectLst", "effectDag", "scene3d", "sp3d")
            .filter { shapeProperties.directElement(DML, it) != null }
            .forEach { warnings += "unsupported-shape-$it" }
        val parsedFill = parseFill(shapeProperties, warnings, "shape")
        val hasExplicitFill = shapeProperties.directElement(DML, "noFill") != null ||
            shapeProperties.directElement(DML, "solidFill") != null
        val fill = if (!hasExplicitFill && hasInheritedStyle && geometry != PptxPresetGeometry.LINE) {
            PptxFill.Solid(requireNotNull(THEME_COLOR_FALLBACKS["accent1"]))
        } else {
            parsedFill
        }
        val line = parseLine(shapeProperties.directElement(DML, "ln"), geometry, warnings)
        val textFrame = shape.directElement(PML, "txBody")?.let { parseTextFrame(it, warnings) }
        if (fill == PptxFill.None && line == null && textFrame == null) {
            warnings += "invisible-shape"
            return null
        }
        return PptxSlideElement.Shape(geometry, fill, line, textFrame, transform)
    }

    private fun parseFill(owner: Element, warnings: MutableSet<String>, label: String): PptxFill {
        if (owner.directElement(DML, "noFill") != null) return PptxFill.None
        val solidFill = owner.directElement(DML, "solidFill") ?: return PptxFill.None
        val color = parseColor(solidFill, warnings)
        if (color == null) warnings += "invalid-$label-solid-color"
        return color?.let(PptxFill::Solid) ?: PptxFill.None
    }

    private fun parseLine(
        line: Element?,
        geometry: PptxPresetGeometry,
        warnings: MutableSet<String>,
    ): PptxLineStyle? {
        if (line == null) {
            return if (geometry == PptxPresetGeometry.LINE) {
                warnings += "line-style-fallback"
                PptxLineStyle(PptxFill.Solid(0xFF000000.toInt()), 12_700)
            } else {
                null
            }
        }
        val fill = parseFill(line, warnings, "line")
        if (fill == PptxFill.None) return null
        val width = line.getAttribute("w").takeIf { it.isNotBlank() }?.toLongOrNull()
        if (line.getAttribute("w").isNotBlank() && width == null) warnings += "invalid-line-width"
        return PptxLineStyle(fill, (width ?: 12_700).coerceAtLeast(1))
    }

    private fun parseTextFrame(body: Element, warnings: MutableSet<String>): PptxTextFrame? {
        val bodyProperties = body.directElement(DML, "bodyPr")
        val paragraphs = body.directElements(DML, "p").map { parseParagraph(it, warnings) }
        if (paragraphs.none { paragraph -> paragraph.runs.any { it.text.isNotEmpty() } }) return null
        return PptxTextFrame(
            paragraphs = paragraphs,
            insetLeftEmu = bodyProperties.emuInset("lIns", 91_440, warnings),
            insetTopEmu = bodyProperties.emuInset("tIns", 45_720, warnings),
            insetRightEmu = bodyProperties.emuInset("rIns", 91_440, warnings),
            insetBottomEmu = bodyProperties.emuInset("bIns", 45_720, warnings),
        )
    }

    private fun parseParagraph(paragraph: Element, warnings: MutableSet<String>): PptxTextParagraph {
        val paragraphProperties = paragraph.directElement(DML, "pPr")
        val alignmentValue = paragraphProperties?.getAttribute("algn").orEmpty()
        val alignment = when (alignmentValue) {
            "", "l" -> PptxParagraphAlignment.LEFT
            "ctr" -> PptxParagraphAlignment.CENTER
            "r" -> PptxParagraphAlignment.RIGHT
            "just", "justLow", "dist", "thaiDist" -> PptxParagraphAlignment.JUSTIFY
            else -> {
                warnings += "paragraph-alignment-fallback:$alignmentValue"
                PptxParagraphAlignment.LEFT
            }
        }
        val defaultProperties = paragraphProperties?.directElement(DML, "defRPr")
        val runs = mutableListOf<PptxTextRun>()
        for (i in 0 until paragraph.childNodes.length) {
            val child = paragraph.childNodes.item(i)
            if (child !is Element || child.namespaceURI != DML) continue
            when (child.localName) {
                "r", "fld" -> {
                    if (child.localName == "fld") warnings += "dynamic-field-rendered-as-text"
                    val text = child.directElement(DML, "t")?.textContent.orEmpty()
                    if (text.isNotEmpty()) runs += parseTextRun(text, child.directElement(DML, "rPr"), defaultProperties, warnings)
                }
                "br" -> runs += parseTextRun("\n", child.directElement(DML, "rPr"), defaultProperties, warnings)
                "pPr", "endParaRPr" -> Unit
                else -> warnings += "unsupported-text-element:${child.localName}"
            }
        }
        return PptxTextParagraph(runs, alignment)
    }

    private fun parseTextRun(
        text: String,
        runProperties: Element?,
        defaultProperties: Element?,
        warnings: MutableSet<String>,
    ): PptxTextRun {
        val properties = runProperties ?: defaultProperties
        if (runProperties == null || properties == null) warnings += "theme-or-master-text-style-fallback"
        val rawSize = properties?.getAttribute("sz").orEmpty()
        val fontSize = rawSize.takeIf(String::isNotBlank)?.toLongOrNull()?.div(100f)
        if (rawSize.isNotBlank() && (fontSize == null || fontSize <= 0f)) warnings += "invalid-font-size"
        val color = properties?.directElement(DML, "solidFill")?.let { parseColor(it, warnings) }
        if (color == null) warnings += "theme-or-master-text-color-fallback"
        val rawTypeface = properties?.directElement(DML, "latin")?.getAttribute("typeface").orEmpty()
        val fontFamily = when {
            rawTypeface.isBlank() -> "sans-serif"
            rawTypeface.startsWith("+") -> {
                warnings += "theme-font-fallback"
                "sans-serif"
            }
            else -> rawTypeface
        }
        return PptxTextRun(
            text = text,
            fontSizePoints = fontSize?.takeIf { it > 0f } ?: 18f,
            argb = color ?: 0xFF000000.toInt(),
            bold = properties.trueAttribute("b"),
            italic = properties.trueAttribute("i"),
            fontFamily = fontFamily,
        )
    }

    private fun parseColor(solidFill: Element, warnings: MutableSet<String>): Int? {
        val colorElement = solidFill.directElements(DML).firstOrNull() ?: return null
        val rgb = when (colorElement.localName) {
            "srgbClr" -> colorElement.getAttribute("val").parseRgb()
            "schemeClr" -> {
                val name = colorElement.getAttribute("val")
                warnings += "theme-color-fallback:$name"
                THEME_COLOR_FALLBACKS[name]
            }
            "sysClr" -> {
                warnings += "system-color-fallback"
                colorElement.getAttribute("lastClr").parseRgb()
                    ?: colorElement.getAttribute("val").parseRgb()
            }
            "prstClr" -> {
                val name = colorElement.getAttribute("val")
                PRESET_COLOR_FALLBACKS[name].also {
                    if (it == null) warnings += "preset-color-fallback:$name"
                }
            }
            else -> {
                warnings += "unsupported-color:${colorElement.localName}"
                null
            }
        } ?: return null
        val alphaValue = colorElement.directElement(DML, "alpha")?.getAttribute("val")?.toIntOrNull()
        val alpha = alphaValue?.coerceIn(0, 100_000)?.times(255)?.div(100_000) ?: 255
        return (alpha shl 24) or (rgb and 0x00FFFFFF)
    }

    private fun Element?.emuInset(name: String, fallback: Long, warnings: MutableSet<String>): Long {
        if (this == null) return fallback
        val raw = getAttribute(name)
        if (raw.isBlank()) return fallback
        val value = raw.toLongOrNull()
        if (value == null || value < 0) {
            warnings += "invalid-text-inset:$name"
            return fallback
        }
        return value
    }

    private fun Element.booleanAttribute(name: String, warnings: MutableSet<String>): Boolean {
        return when (val value = getAttribute(name)) {
            "", "0", "false", "off" -> false
            "1", "true", "on" -> true
            else -> {
                warnings += "invalid-boolean-attribute:$name"
                false
            }
        }
    }

    private fun Element?.trueAttribute(name: String): Boolean =
        this?.getAttribute(name) in setOf("1", "true", "on")

    private fun String.parseRgb(): Int? {
        if (length != 6 || any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return null
        return toIntOrNull(16)?.let { 0xFF000000.toInt() or it }
    }

    private fun validateArchive(zip: ZipFile, limits: PptxReadLimits) {
        val entries = zip.entries().asSequence().filterNot { it.isDirectory }.toList()
        if (entries.size > limits.maxEntries) security("PPTX has too many ZIP entries")
        var total = 0L
        val names = HashSet<String>()
        entries.forEach { entry ->
            validateEntryName(entry.name)
            if (!names.add(entry.name)) security("PPTX contains duplicate ZIP path: ${entry.name}")
            val size = entry.size
            val compressed = entry.compressedSize
            if (size < 0 || compressed < 0) security("PPTX contains an entry with unknown size")
            total = safeAdd(total, size)
            if (total > limits.maxTotalUncompressedBytes) security("PPTX expanded size is too large")
            if (compressed == 0L && size > 0L) security("PPTX contains an invalid compression ratio")
            if (compressed > 0 && size / compressed > limits.maxCompressionRatio) security("PPTX compression ratio is too high")
            val max = if (entry.name.endsWith(".xml") || entry.name.endsWith(".rels")) limits.maxXmlBytes else limits.maxImageBytes
            if (size > max) security("PPTX part is too large: ${entry.name}")
        }
    }

    private fun validateEntryName(name: String) {
        if (name.isBlank() || '\u0000' in name || '\\' in name || name.startsWith('/') || DRIVE_PREFIX.matches(name)) {
            security("Unsafe ZIP path: $name")
        }
        var depth = 0
        name.split('/').forEach {
            when (it) {
                "", "." -> security("Unsafe ZIP path: $name")
                ".." -> if (--depth < 0) security("ZIP path escapes package: $name")
                else -> depth++
            }
        }
    }

    private fun relationships(zip: ZipFile, path: String, limits: PptxReadLimits): Map<String, Relationship> {
        val document = zip.xml(path, limits)
        val result = linkedMapOf<String, Relationship>()
        document.elements(REL_PACKAGE, "Relationship").forEach { element ->
            val id = element.getAttribute("Id").required("Relationship Id")
            if (id in result) malformed("Duplicate relationship Id: $id")
            result[id] = Relationship(
                type = element.getAttribute("Type").required("Relationship Type"),
                target = element.getAttribute("Target").required("Relationship Target"),
                external = element.getAttribute("TargetMode").equals("External", ignoreCase = true),
            )
        }
        return result
    }

    private fun relationshipsIfPresent(zip: ZipFile, path: String, limits: PptxReadLimits): Map<String, Relationship> =
        if (zip.getEntry(path) == null) emptyMap() else relationships(zip, path, limits)

    private fun ZipFile.xml(path: String, limits: PptxReadLimits): Document {
        val entry = getEntry(path) ?: malformed("Missing PPTX part: $path")
        if (entry.size > limits.maxXmlBytes) security("XML part is too large: $path")
        val bytes = getInputStream(entry).use { it.readLimitedBytes(limits.maxXmlBytes) }
        return parseSecurePackageXml(bytes) { message -> security(message) }
    }

    private fun resolvePart(basePart: String, target: String): String {
        if ('\u0000' in target || '\\' in target) security("Unsafe relationship target")
        val result: MutableList<String> = if (target.startsWith('/')) {
            mutableListOf()
        } else {
            basePart.substringBeforeLast('/', "").split('/').filter { it.isNotEmpty() }.toMutableList()
        }
        target.removePrefix("/").split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (result.isEmpty()) security("Relationship target escapes package") else result.removeAt(result.lastIndex)
                else -> result += segment
            }
        }
        val path = result.joinToString("/")
        validateEntryName(path)
        return path
    }

    private fun relationshipPartFor(part: String): String =
        part.substringBeforeLast('/') + "/_rels/" + part.substringAfterLast('/') + ".rels"

    private fun Document.elements(namespace: String, localName: String): List<Element> {
        val nodes = getElementsByTagNameNS(namespace, localName)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.elements(namespace: String, localName: String): List<Element> {
        val nodes = getElementsByTagNameNS(namespace, localName)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.directElements(namespace: String, localName: String? = null): List<Element> = buildList {
        for (index in 0 until childNodes.length) {
            val child = childNodes.item(index)
            if (child is Element && child.namespaceURI == namespace && (localName == null || child.localName == localName)) {
                add(child)
            }
        }
    }

    private fun Element.directElement(namespace: String, localName: String): Element? =
        directElements(namespace, localName).firstOrNull()

    private fun Document.singleElement(namespace: String, localName: String): Element =
        elements(namespace, localName).singleOrNull() ?: malformed("Expected one $localName element")

    private fun Element.longAttribute(name: String): Long =
        getAttribute(name).toLongOrNull() ?: malformed("Invalid $name attribute")

    private fun Element.hasNonZeroCrop(): Boolean =
        listOf("l", "t", "r", "b").any { getAttribute(it).toLongOrNull()?.let { value -> value != 0L } == true }

    private fun safeAdd(left: Long, right: Long): Long {
        if (right > Long.MAX_VALUE - left) security("PPTX size overflow")
        return left + right
    }

    private fun String.required(label: String): String = takeIf { it.isNotBlank() } ?: malformed("Missing $label")

    private data class Relationship(val type: String, val target: String, val external: Boolean)

    private class SecurityLimitException(message: String) : IOException(message)
    private fun security(message: String): Nothing = throw SecurityLimitException(message)
    private fun malformed(message: String): Nothing = throw IOException(message)

    private val DRIVE_PREFIX = Regex("^[A-Za-z]:.*")
    private val THEME_COLOR_FALLBACKS = mapOf(
        "dk1" to 0xFF000000.toInt(),
        "tx1" to 0xFF000000.toInt(),
        "lt1" to 0xFFFFFFFF.toInt(),
        "bg1" to 0xFFFFFFFF.toInt(),
        "dk2" to 0xFF1F1F1F.toInt(),
        "tx2" to 0xFF1F1F1F.toInt(),
        "lt2" to 0xFFF5F5F5.toInt(),
        "bg2" to 0xFFF5F5F5.toInt(),
        "accent1" to 0xFF6750A4.toInt(),
        "accent2" to 0xFF625B71.toInt(),
        "accent3" to 0xFF7D5260.toInt(),
        "accent4" to 0xFF386A20.toInt(),
        "accent5" to 0xFF00639B.toInt(),
        "accent6" to 0xFF8C5000.toInt(),
        "hlink" to 0xFF0000FF.toInt(),
        "folHlink" to 0xFF800080.toInt(),
        "phClr" to 0xFF000000.toInt(),
    )
    private val PRESET_COLOR_FALLBACKS = mapOf(
        "black" to 0xFF000000.toInt(),
        "white" to 0xFFFFFFFF.toInt(),
        "red" to 0xFFFF0000.toInt(),
        "green" to 0xFF008000.toInt(),
        "blue" to 0xFF0000FF.toInt(),
        "yellow" to 0xFFFFFF00.toInt(),
        "gray" to 0xFF808080.toInt(),
    )
    private const val PML = "http://schemas.openxmlformats.org/presentationml/2006/main"
    private const val DML = "http://schemas.openxmlformats.org/drawingml/2006/main"
    private const val REL_OFFICE = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val REL_PACKAGE = "http://schemas.openxmlformats.org/package/2006/relationships"
}

private fun InputStream.readLimitedBytes(maxBytes: Long): ByteArray {
    val buffer = ByteArray(16 * 1024)
    val output = java.io.ByteArrayOutputStream()
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw IOException("PPTX part exceeded its read limit")
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
