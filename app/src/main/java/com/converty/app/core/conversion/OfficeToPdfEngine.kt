package com.converty.app.core.conversion

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.converty.app.core.model.FileFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

class OfficeToPdfEngine(
    private val context: Context,
    private val sourceFormat: FileFormat,
) : ConversionEngine {
    init {
        require(sourceFormat in SUPPORTED_FORMATS)
    }

    override val capabilities = ConversionCapabilities(
        sourceFormat = sourceFormat.toEngineFormat(),
        destinationFormat = DocumentFormat.PDF,
        supportsItemSelection = false,
        preservesEditableText = true,
        notes = setOf("basic-text-layout", "formatting-may-differ"),
    )

    override fun inspect(
        source: android.net.Uri,
        cancellation: ConversionCancellation,
    ): DocumentInspectionResult {
        if (cancellation.isCancellationRequested()) return DocumentInspectionResult.Cancelled
        var temporary: File? = null
        return try {
            temporary = context.copyUriToTemporaryFile(
                source,
                ".${sourceFormat.primaryExtension}",
                cancellation,
                MAX_SOURCE_BYTES,
            )
            val extracted = OfficeTextExtractor.extract(temporary, sourceFormat)
            if (extracted.sections.none { it.lines.any(String::isNotBlank) }) {
                DocumentInspectionResult.Failure(
                    ErrorCode.MALFORMED_DOCUMENT,
                    "Document contains no renderable text",
                )
            } else {
                DocumentInspectionResult.Success(
                    DocumentInspection(
                        sourceFormat.toEngineFormat(),
                        1,
                        extracted.warnings,
                    ),
                )
            }
        } catch (error: OfficeSecurityException) {
            DocumentInspectionResult.Failure(ErrorCode.SECURITY_LIMIT_EXCEEDED, error.message ?: "Unsafe document", error)
        } catch (error: IOException) {
            DocumentInspectionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, error.message ?: "Document inspection failed", error)
        } finally {
            temporary?.delete()
        }
    }

    override fun convert(
        request: ConversionRequest,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        var sourceFile: File? = null
        var outputFile: File? = null
        return try {
            sourceFile = context.copyUriToTemporaryFile(
                request.source,
                ".${sourceFormat.primaryExtension}",
                cancellation,
                MAX_SOURCE_BYTES,
            )
            val extracted = OfficeTextExtractor.extract(sourceFile, sourceFormat)
            if (extracted.sections.none { it.lines.any(String::isNotBlank) }) {
                return ConversionResult.Unsupported(setOf("document-without-renderable-text"), extracted.warnings)
            }
            outputFile = File.createTempFile("converty-office-", ".pdf", context.cacheDir)
            val pageCount = renderPdf(outputFile, extracted, cancellation, onProgress)
            val bytes = context.copyTemporaryFileToUri(outputFile, request.destination, cancellation)
            ConversionResult.Success(
                output = request.destination,
                convertedItems = pageCount,
                bytesWritten = bytes,
                warnings = extracted.warnings,
            )
        } catch (_: ConversionCancelledException) {
            ConversionResult.Cancelled
        } catch (error: OfficeSecurityException) {
            ConversionResult.Failure(ErrorCode.SECURITY_LIMIT_EXCEEDED, error.message ?: "Unsafe document", error)
        } catch (error: DestinationNotWritableException) {
            ConversionResult.Failure(ErrorCode.DESTINATION_NOT_WRITABLE, error.message ?: "Destination cannot be written", error)
        } catch (error: OutOfMemoryError) {
            ConversionResult.Failure(ErrorCode.OUT_OF_MEMORY, "Document is too large to render", error)
        } catch (error: IOException) {
            ConversionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "Office conversion failed", error)
        } catch (error: RuntimeException) {
            ConversionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, error.message ?: "Malformed document", error)
        } finally {
            sourceFile?.delete()
            outputFile?.delete()
        }
    }

    private fun renderPdf(
        output: File,
        document: ExtractedOfficeDocument,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ): Int {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 35, 50)
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
        }
        val wrappedSections = document.sections.map { section ->
            section.copy(lines = section.lines.flatMap { wrap(it, bodyPaint, PAGE_WIDTH - MARGIN * 2) })
        }
        val estimatedPages = wrappedSections.sumOf { section ->
            maxOf(1, (section.lines.size + LINES_PER_PAGE - 1) / LINES_PER_PAGE)
        }
        var pageNumber = 0
        val pdf = PdfDocument()
        try {
            wrappedSections.forEach { section ->
                val lines = section.lines.ifEmpty { listOf("") }
                lines.chunked(LINES_PER_PAGE).forEach { pageLines ->
                    if (cancellation.isCancellationRequested()) throw ConversionCancelledException()
                    onProgress(ConversionProgress(pageNumber, estimatedPages, ConversionProgress.Stage.RENDERING))
                    pageNumber++
                    val page = pdf.startPage(
                        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
                    )
                    var y = MARGIN.toFloat()
                    page.canvas.drawText(section.title.take(120), MARGIN.toFloat(), y + titlePaint.textSize, titlePaint)
                    y += 36f
                    pageLines.forEach { line ->
                        page.canvas.drawText(line, MARGIN.toFloat(), y + bodyPaint.textSize, bodyPaint)
                        y += LINE_HEIGHT
                    }
                    pdf.finishPage(page)
                    onProgress(ConversionProgress(pageNumber, estimatedPages, ConversionProgress.Stage.PACKAGING))
                }
            }
            FileOutputStream(output).use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        return pageNumber
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Int): List<String> {
        if (text.isBlank()) return listOf("")
        val result = mutableListOf<String>()
        var remaining = text.replace('\t', ' ').trim()
        while (remaining.isNotEmpty()) {
            var count = paint.breakText(remaining, true, maxWidth.toFloat(), null)
            if (count <= 0) count = 1
            if (count < remaining.length) {
                val whitespace = remaining.lastIndexOf(' ', count - 1)
                if (whitespace > 0) count = whitespace
            }
            result += remaining.take(count).trimEnd()
            remaining = remaining.drop(count).trimStart()
        }
        return result
    }

    private companion object {
        val SUPPORTED_FORMATS = setOf(
            FileFormat.DOCX,
            FileFormat.XLSX,
            FileFormat.ODT,
            FileFormat.ODS,
            FileFormat.ODP,
        )
        const val MAX_SOURCE_BYTES = 128L * 1024 * 1024
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 42
        const val LINE_HEIGHT = 16f
        const val LINES_PER_PAGE = 45
    }
}

internal data class OfficeSection(val title: String, val lines: List<String>)
internal data class ExtractedOfficeDocument(
    val sections: List<OfficeSection>,
    val warnings: List<ConversionWarning>,
)

internal class OfficeSecurityException(message: String) : IOException(message)

internal object OfficeTextExtractor {
    fun extract(file: File, format: FileFormat): ExtractedOfficeDocument = ZipFile(file).use { zip ->
        validateArchive(zip)
        val sections = when (format) {
            FileFormat.DOCX -> extractDocx(zip)
            FileFormat.XLSX -> extractXlsx(zip)
            FileFormat.ODT -> extractOdt(zip)
            FileFormat.ODS -> extractOds(zip)
            FileFormat.ODP -> extractOdp(zip)
            else -> error("Unsupported office format $format")
        }
        ExtractedOfficeDocument(
            sections = sections,
            warnings = listOf(
                ConversionWarning(
                    code = "basic-office-layout",
                    message = "Complex formatting may differ from the source document",
                ),
            ),
        )
    }

    private fun extractDocx(zip: ZipFile): List<OfficeSection> {
        val document = zip.xml("word/document.xml")
        val paragraphs = document.elements("p").mapNotNull { paragraph ->
            paragraph.elements("t").joinToString("") { it.textContent }.trim().takeIf(String::isNotEmpty)
        }
        return listOf(OfficeSection("Document", paragraphs))
    }

    private fun extractXlsx(zip: ZipFile): List<OfficeSection> {
        val shared = zip.getEntry("xl/sharedStrings.xml")?.let {
            zip.xml(it.name).elements("si").map { item ->
                item.elements("t").joinToString("") { it.textContent }
            }
        }.orEmpty()
        val sheets = zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }
            .sortedBy { it.name.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE }
            .toList()
        return sheets.mapIndexed { index, entry ->
            val rows = zip.xml(entry.name).elements("row").map { row ->
                val cells = mutableMapOf<Int, String>()
                row.directElements("c").forEach { cell ->
                    val reference = cell.getAttribute("r")
                    val column = excelColumn(reference)
                    val raw = cell.elements("v").firstOrNull()?.textContent.orEmpty()
                    val value = when (cell.getAttribute("t")) {
                        "s" -> raw.toIntOrNull()?.let(shared::getOrNull).orEmpty()
                        "inlineStr" -> cell.elements("t").joinToString("") { it.textContent }
                        else -> raw
                    }
                    cells[column] = value
                }
                if (cells.isEmpty()) "" else (0..cells.keys.max()).joinToString("    ") { cells[it].orEmpty() }
            }
            OfficeSection("Sheet ${index + 1}", rows)
        }
    }

    private fun extractOdt(zip: ZipFile): List<OfficeSection> {
        val document = zip.xml("content.xml")
        val lines = document.documentElement.descendantElements()
            .filter { it.localName == "h" || it.localName == "p" }
            .mapNotNull { it.textContent.trim().takeIf(String::isNotEmpty) }
            .toList()
        return listOf(OfficeSection("Document", lines))
    }

    private fun extractOds(zip: ZipFile): List<OfficeSection> {
        val document = zip.xml("content.xml")
        return document.elements("table").mapIndexed { index, table ->
            val name = table.getAttributeNS("urn:oasis:names:tc:opendocument:xmlns:table:1.0", "name")
                .ifBlank { "Sheet ${index + 1}" }
            val rows = table.elements("table-row").map { row ->
                row.directElements("table-cell").joinToString("    ") { cell ->
                    cell.elements("p").joinToString(" ") { it.textContent.trim() }
                }
            }
            OfficeSection(name, rows)
        }
    }

    private fun extractOdp(zip: ZipFile): List<OfficeSection> {
        val document = zip.xml("content.xml")
        return document.elements("page").mapIndexed { index, page ->
            val name = page.getAttributeNS("urn:oasis:names:tc:opendocument:xmlns:drawing:1.0", "name")
                .ifBlank { "Slide ${index + 1}" }
            val lines = page.descendantElements()
                .filter { it.localName == "h" || it.localName == "p" }
                .mapNotNull { it.textContent.trim().takeIf(String::isNotEmpty) }
                .toList()
            OfficeSection(name, lines)
        }
    }

    private fun validateArchive(zip: ZipFile) {
        val entries = zip.entries().asSequence().filterNot(ZipEntry::isDirectory).toList()
        if (entries.size > 4096) throw OfficeSecurityException("Document has too many ZIP entries")
        var total = 0L
        val names = hashSetOf<String>()
        entries.forEach { entry ->
            if (entry.name.startsWith('/') || '\\' in entry.name || entry.name.split('/').any { it == ".." }) {
                throw OfficeSecurityException("Document contains an unsafe ZIP path")
            }
            if (!names.add(entry.name)) throw OfficeSecurityException("Document contains duplicate ZIP paths")
            if (entry.size < 0 || entry.compressedSize < 0) throw OfficeSecurityException("Document has unknown ZIP sizes")
            total += entry.size
            if (total > 256L * 1024 * 1024) throw OfficeSecurityException("Document expands beyond the safe limit")
            if (entry.compressedSize > 0 && entry.size / entry.compressedSize > 100) {
                throw OfficeSecurityException("Document compression ratio is unsafe")
            }
        }
    }

    private fun ZipFile.xml(path: String): Document {
        val entry = getEntry(path) ?: throw IOException("Required document part is missing: $path")
        if (entry.size > 16L * 1024 * 1024) throw OfficeSecurityException("XML part is too large: $path")
        val bytes = getInputStream(entry).use { input ->
            ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(32 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > 16 * 1024 * 1024) {
                        throw OfficeSecurityException("XML part is too large: $path")
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        }
        return parseSecurePackageXml(bytes) { message -> throw OfficeSecurityException(message) }
    }

    private fun Document.elements(localName: String): List<Element> =
        getElementsByTagNameNS("*", localName).asElements()

    private fun Element.elements(localName: String): List<Element> =
        getElementsByTagNameNS("*", localName).asElements()

    private fun Element.directElements(localName: String): List<Element> = buildList {
        for (index in 0 until childNodes.length) {
            val node = childNodes.item(index)
            if (node is Element && node.localName == localName) add(node)
        }
    }

    private fun Element.descendantElements(): Sequence<Element> = sequence {
        for (index in 0 until childNodes.length) {
            val node = childNodes.item(index)
            if (node is Element) {
                yield(node)
                yieldAll(node.descendantElements())
            }
        }
    }

    private fun org.w3c.dom.NodeList.asElements(): List<Element> = buildList {
        for (index in 0 until length) (item(index) as? Element)?.let(::add)
    }

    private fun excelColumn(reference: String): Int {
        var result = 0
        reference.takeWhile(Char::isLetter).uppercase().forEach { char ->
            result = result * 26 + (char - 'A' + 1)
        }
        return (result - 1).coerceAtLeast(0)
    }
}
