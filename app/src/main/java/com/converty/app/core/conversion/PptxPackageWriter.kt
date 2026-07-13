package com.converty.app.core.conversion

import java.io.Closeable
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToLong

data class EmuSize(val width: Long, val height: Long) {
    init {
        require(width > 0 && height > 0)
    }
}

data class EmuRect(val x: Long, val y: Long, val width: Long, val height: Long)

internal object PlacementCalculator {
    fun calculate(slide: EmuSize, imageWidth: Int, imageHeight: Int, fit: ImageFit): EmuRect {
        require(imageWidth > 0 && imageHeight > 0)
        if (fit == ImageFit.STRETCH) return EmuRect(0, 0, slide.width, slide.height)
        val scaleX = slide.width.toDouble() / imageWidth
        val scaleY = slide.height.toDouble() / imageHeight
        val scale = if (fit == ImageFit.COVER) maxOf(scaleX, scaleY) else minOf(scaleX, scaleY)
        val width = (imageWidth * scale).roundToLong()
        val height = (imageHeight * scale).roundToLong()
        return EmuRect((slide.width - width) / 2, (slide.height - height) / 2, width, height)
    }
}

/** Small deterministic PresentationML writer for one raster image per slide. */
class PptxPackageWriter(
    output: OutputStream,
    val slideSize: EmuSize,
) : Closeable {
    private val zip = ZipOutputStream(output)
    private val slides = mutableListOf<SlideRecord>()
    private var finished = false

    fun addImageSlide(
        imageBytes: ByteArray,
        mimeType: String,
        imageWidth: Int,
        imageHeight: Int,
        placement: EmuRect,
    ) {
        check(!finished) { "PPTX writer is already finished" }
        require(imageBytes.isNotEmpty())
        require(imageWidth > 0 && imageHeight > 0)
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/jpeg" -> "jpeg"
            else -> throw IllegalArgumentException("Unsupported image type: $mimeType")
        }
        val number = slides.size + 1
        put("ppt/media/image$number.$extension", imageBytes)
        put("ppt/slides/slide$number.xml", slideXml(number, placement))
        put("ppt/slides/_rels/slide$number.xml.rels", slideRelationships(number, extension))
        slides += SlideRecord(number, extension)
    }

    fun finish() {
        if (finished) return
        require(slides.isNotEmpty()) { "A presentation must contain at least one slide" }
        writeSharedPresentationParts()
        put("[Content_Types].xml", contentTypes())
        put("_rels/.rels", rootRelationships())
        put("ppt/presentation.xml", presentationXml())
        put("ppt/_rels/presentation.xml.rels", presentationRelationships())
        zip.finish()
        finished = true
    }

    override fun close() {
        try {
            // An exception/cancellation may happen before the first slide. Do not mask it here.
            if (slides.isNotEmpty()) finish()
        } finally {
            zip.close()
        }
    }

    private fun put(path: String, content: String) = put(path, content.toByteArray(StandardCharsets.UTF_8))

    private fun put(path: String, content: ByteArray) {
        val entry = ZipEntry(path).apply { time = 0L }
        zip.putNextEntry(entry)
        zip.write(content)
        zip.closeEntry()
    }

    private fun contentTypes(): String = buildString {
        append(XML_HEADER)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        if (slides.any { it.extension == "png" }) append("<Default Extension=\"png\" ContentType=\"image/png\"/>")
        if (slides.any { it.extension == "jpeg" }) append("<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/>")
        append("<Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>")
        append("<Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml\"/>")
        append("<Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml\"/>")
        append("<Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.theme+xml\"/>")
        slides.forEach { append("<Override PartName=\"/ppt/slides/slide${it.number}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>") }
        append("</Types>")
    }

    private fun rootRelationships(): String = XML_HEADER +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"ppt/presentation.xml\"/>" +
        "</Relationships>"

    private fun presentationXml(): String = buildString {
        append(XML_HEADER)
        append("<p:presentation xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">")
        append("<p:sldMasterIdLst><p:sldMasterId id=\"2147483648\" r:id=\"rId1\"/></p:sldMasterIdLst><p:sldIdLst>")
        slides.forEachIndexed { index, slide -> append("<p:sldId id=\"${256 + index}\" r:id=\"rId${slide.number + 1}\"/>") }
        append("</p:sldIdLst><p:sldSz cx=\"${slideSize.width}\" cy=\"${slideSize.height}\" type=\"custom\"/>")
        append("<p:notesSz cx=\"6858000\" cy=\"9144000\"/></p:presentation>")
    }

    private fun presentationRelationships(): String = buildString {
        append(XML_HEADER)
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        append("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"slideMasters/slideMaster1.xml\"/>")
        slides.forEach { append("<Relationship Id=\"rId${it.number + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide${it.number}.xml\"/>") }
        append("</Relationships>")
    }

    private fun slideRelationships(number: Int, extension: String): String = XML_HEADER +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/image$number.$extension\"/>" +
        "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>" +
        "</Relationships>"

    private fun slideXml(number: Int, p: EmuRect): String = XML_HEADER +
        "<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">" +
        "<p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>" +
        "<p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"${slideSize.width}\" cy=\"${slideSize.height}\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"${slideSize.width}\" cy=\"${slideSize.height}\"/></a:xfrm></p:grpSpPr>" +
        "<p:pic><p:nvPicPr><p:cNvPr id=\"${number + 1}\" name=\"Page $number\"/><p:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></p:cNvPicPr><p:nvPr/></p:nvPicPr>" +
        "<p:blipFill><a:blip r:embed=\"rId1\"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>" +
        "<p:spPr><a:xfrm><a:off x=\"${p.x}\" y=\"${p.y}\"/><a:ext cx=\"${p.width}\" cy=\"${p.height}\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom><a:noFill/></p:spPr>" +
        "</p:pic></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>"

    private data class SlideRecord(val number: Int, val extension: String)

    private fun writeSharedPresentationParts() {
        put("ppt/slideMasters/slideMaster1.xml", slideMasterXml())
        put("ppt/slideMasters/_rels/slideMaster1.xml.rels", slideMasterRelationships())
        put("ppt/slideLayouts/slideLayout1.xml", slideLayoutXml())
        put("ppt/slideLayouts/_rels/slideLayout1.xml.rels", slideLayoutRelationships())
        put("ppt/theme/theme1.xml", themeXml())
    }

    private fun slideMasterXml(): String = XML_HEADER +
        "<p:sldMaster xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">" +
        "<p:cSld name=\"Converty Master\">${emptyShapeTree()}</p:cSld>" +
        "<p:clrMap accent1=\"accent1\" accent2=\"accent2\" accent3=\"accent3\" accent4=\"accent4\" accent5=\"accent5\" accent6=\"accent6\" bg1=\"lt1\" bg2=\"lt2\" folHlink=\"folHlink\" hlink=\"hlink\" tx1=\"dk1\" tx2=\"dk2\"/>" +
        "<p:sldLayoutIdLst><p:sldLayoutId id=\"2147483649\" r:id=\"rId1\"/></p:sldLayoutIdLst></p:sldMaster>"

    private fun slideLayoutXml(): String = XML_HEADER +
        "<p:sldLayout xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" type=\"blank\" preserve=\"1\">" +
        "<p:cSld name=\"Blank\">${emptyShapeTree()}</p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>"

    private fun emptyShapeTree(): String =
        "<p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>" +
        "<p:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"0\" cy=\"0\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"0\" cy=\"0\"/></a:xfrm></p:grpSpPr></p:spTree>"

    private fun slideMasterRelationships(): String = XML_HEADER +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout\" Target=\"../slideLayouts/slideLayout1.xml\"/>" +
        "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme\" Target=\"../theme/theme1.xml\"/></Relationships>"

    private fun slideLayoutRelationships(): String = XML_HEADER +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster\" Target=\"../slideMasters/slideMaster1.xml\"/></Relationships>"

    private fun themeXml(): String = XML_HEADER +
        "<a:theme xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" name=\"Converty\"><a:themeElements>" +
        "<a:clrScheme name=\"Converty\"><a:dk1><a:srgbClr val=\"000000\"/></a:dk1><a:lt1><a:srgbClr val=\"FFFFFF\"/></a:lt1>" +
        "<a:dk2><a:srgbClr val=\"1F1F1F\"/></a:dk2><a:lt2><a:srgbClr val=\"F5F5F5\"/></a:lt2>" +
        "<a:accent1><a:srgbClr val=\"6750A4\"/></a:accent1><a:accent2><a:srgbClr val=\"625B71\"/></a:accent2>" +
        "<a:accent3><a:srgbClr val=\"7D5260\"/></a:accent3><a:accent4><a:srgbClr val=\"386A20\"/></a:accent4>" +
        "<a:accent5><a:srgbClr val=\"00639B\"/></a:accent5><a:accent6><a:srgbClr val=\"8C5000\"/></a:accent6>" +
        "<a:hlink><a:srgbClr val=\"0000FF\"/></a:hlink><a:folHlink><a:srgbClr val=\"800080\"/></a:folHlink></a:clrScheme>" +
        "<a:fontScheme name=\"Converty\"><a:majorFont><a:latin typeface=\"Arial\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:majorFont>" +
        "<a:minorFont><a:latin typeface=\"Arial\"/><a:ea typeface=\"\"/><a:cs typeface=\"\"/></a:minorFont></a:fontScheme>" +
        "<a:fmtScheme name=\"Converty\"><a:fillStyleLst>${solidFill()}${solidFill()}${solidFill()}</a:fillStyleLst>" +
        "<a:lnStyleLst>${lineStyle(6350)}${lineStyle(12700)}${lineStyle(19050)}</a:lnStyleLst>" +
        "<a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst>" +
        "<a:bgFillStyleLst>${solidFill()}${solidFill()}${solidFill()}</a:bgFillStyleLst></a:fmtScheme>" +
        "</a:themeElements></a:theme>"

    private fun solidFill(): String = "<a:solidFill><a:schemeClr val=\"phClr\"/></a:solidFill>"

    private fun lineStyle(width: Int): String =
        "<a:ln w=\"$width\" cap=\"flat\" cmpd=\"sng\" algn=\"ctr\">${solidFill()}<a:prstDash val=\"solid\"/><a:miter lim=\"800000\"/></a:ln>"

    private companion object {
        const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    }
}
