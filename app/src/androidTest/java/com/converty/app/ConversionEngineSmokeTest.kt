package com.converty.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.converty.app.core.conversion.ConversionRequest
import com.converty.app.core.conversion.ConversionResult
import com.converty.app.core.conversion.ImageQuality
import com.converty.app.core.conversion.ImageTranscodeEngine
import com.converty.app.core.conversion.IndexedDestination
import com.converty.app.core.conversion.MultiOutputConversionRequest
import com.converty.app.core.conversion.MultiOutputConversionResult
import com.converty.app.core.conversion.OfficeToPdfEngine
import com.converty.app.core.conversion.PdfThumbnailEngine
import com.converty.app.core.conversion.PdfToPptxEngine
import com.converty.app.core.conversion.PdfToRasterPagesEngine
import com.converty.app.core.conversion.PdfToTiffEngine
import com.converty.app.core.conversion.PptxToPdfEngine
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.FileFormat
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversionEngineSmokeTest {
    private lateinit var context: Context
    private lateinit var testDirectory: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testDirectory = File(context.cacheDir, "engine-smoke-${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        testDirectory.deleteRecursively()
    }

    @Test
    fun pdfRasterTiffAndThumbnailProduceReadableFiles() {
        val source = createPdf("source.pdf", pages = 2)
        val pngFiles = listOf(file("page-1.png"), file("page-2.png"))
        val pngResult = PdfToRasterPagesEngine(context, FileFormat.PNG).convertMultiple(
            MultiOutputConversionRequest(
                source = source.uri(),
                destinations = pngFiles.mapIndexed { index, output -> IndexedDestination(index, output.uri()) },
                dpi = 72,
            ),
        )
        assertTrue(pngResult is MultiOutputConversionResult.Success)
        pngFiles.forEach { assertSignature(it, 0x89, 0x50, 0x4E, 0x47) }

        val jpg = file("page-2.jpg")
        val jpgResult = PdfToRasterPagesEngine(context, FileFormat.JPG).convertMultiple(
            MultiOutputConversionRequest(
                source = source.uri(),
                destinations = listOf(IndexedDestination(1, jpg.uri())),
                imageQuality = ImageQuality.HIGH,
                dpi = 96,
            ),
        )
        assertTrue(jpgResult is MultiOutputConversionResult.Success)
        assertSignature(jpg, 0xFF, 0xD8)

        val tiff = file("pages.tiff")
        assertSuccess(
            PdfToTiffEngine(context).convert(
                ConversionRequest(source.uri(), tiff.uri(), dpi = 72),
            ),
        )
        assertSignature(tiff, 'I'.code, 'I'.code, 42, 0)

        val thumbnail = file("thumbnail.jpg")
        val thumbnailResult = assertSuccess(
            PdfThumbnailEngine(context).convert(
                ConversionRequest(source.uri(), thumbnail.uri(), imageQuality = ImageQuality.HIGH),
            ),
        )
        assertEquals(1, thumbnailResult.convertedItems)
        assertSignature(thumbnail, 0xFF, 0xD8)
    }

    @Test
    fun rasterBmpSvgAndTiffPathsProduceRealImages() {
        val png = createPng("source.png")
        val jpg = transcode(ConversionDirection.PNG_TO_JPG, png, "from-png.jpg")
        assertSignature(jpg, 0xFF, 0xD8)
        val webp = transcode(ConversionDirection.JPG_TO_WEBP, jpg, "from-jpg.webp")
        assertAscii(webp, 0, "RIFF")
        assertAscii(webp, 8, "WEBP")
        val pngAgain = transcode(ConversionDirection.WEBP_TO_PNG, webp, "from-webp.png")
        assertSignature(pngAgain, 0x89, 0x50, 0x4E, 0x47)

        val bmp = createBmp("source.bmp")
        val bmpPng = transcode(ConversionDirection.BMP_TO_PNG, bmp, "from-bmp.png")
        assertSignature(bmpPng, 0x89, 0x50, 0x4E, 0x47)

        val svg = file("source.svg").apply {
            writeText(
                """
                <svg xmlns="http://www.w3.org/2000/svg" width="40" height="30">
                    <defs><rect width="40" height="30" fill="#ff0000"/></defs>
                    <rect x="5" y="5" width="30" height="20" fill="#3366ff"/>
                </svg>
                """.trimIndent(),
            )
        }
        val svgPng = transcode(ConversionDirection.SVG_TO_PNG, svg, "from-svg.png")
        assertSignature(svgPng, 0x89, 0x50, 0x4E, 0x47)

        val onePagePdf = createPdf("one-page.pdf", pages = 1)
        val tiff = file("source.tiff")
        assertSuccess(PdfToTiffEngine(context).convert(ConversionRequest(onePagePdf.uri(), tiff.uri(), dpi = 72)))
        val tiffPng = transcode(ConversionDirection.TIFF_TO_PNG, tiff, "from-tiff.png")
        assertSignature(tiffPng, 0x89, 0x50, 0x4E, 0x47)
    }

    @Test
    fun platformHeicAndAvifCodecsProduceEverySupportedTarget() {
        val heic = copyTestAsset("aosp_heifwriter_input.heic", "source.heic")
        val heicJpg = transcode(ConversionDirection.HEIC_HEIF_TO_JPG, heic, "from-heic.jpg")
        assertSignature(heicJpg, 0xFF, 0xD8)
        val heicPng = transcode(ConversionDirection.HEIC_HEIF_TO_PNG, heic, "from-heic.png")
        assertSignature(heicPng, 0x89, 0x50, 0x4E, 0x47)

        val avif = copyTestAsset("aosp_avif_yuv_420_8bit.avif", "source.avif")
        val avifPng = transcode(ConversionDirection.AVIF_TO_PNG, avif, "from-avif.png")
        assertSignature(avifPng, 0x89, 0x50, 0x4E, 0x47)
        val avifJpg = transcode(ConversionDirection.AVIF_TO_JPG, avif, "from-avif.jpg")
        assertSignature(avifJpg, 0xFF, 0xD8)
        val avifWebp = transcode(ConversionDirection.AVIF_TO_WEBP, avif, "from-avif.webp")
        assertAscii(avifWebp, 0, "RIFF")
        assertAscii(avifWebp, 8, "WEBP")
    }

    @Test
    fun allOfficeAndOdfSourcesProducePdfWithExplicitLayoutWarning() {
        val fixtures = mapOf(
            FileFormat.DOCX to arrayOf(
                "word/document.xml" to "<w:document xmlns:w=\"urn:w\"><w:body><w:p><w:t>Document</w:t></w:p></w:body></w:document>",
            ),
            FileFormat.XLSX to arrayOf(
                "xl/worksheets/sheet1.xml" to "<worksheet xmlns=\"urn:x\"><row><c r=\"A1\"><v>42</v></c></row></worksheet>",
            ),
            FileFormat.ODT to arrayOf(
                "content.xml" to "<o:root xmlns:o=\"urn:o\" xmlns:t=\"urn:t\"><t:p>Text</t:p></o:root>",
            ),
            FileFormat.ODS to arrayOf(
                "content.xml" to "<o:root xmlns:o=\"urn:o\" xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" xmlns:t=\"urn:t\"><table:table table:name=\"Sheet\"><table:table-row><table:table-cell><t:p>Cell</t:p></table:table-cell></table:table-row></table:table></o:root>",
            ),
            FileFormat.ODP to arrayOf(
                "content.xml" to "<o:root xmlns:o=\"urn:o\" xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" xmlns:t=\"urn:t\"><draw:page draw:name=\"Slide\"><t:p>Title</t:p></draw:page></o:root>",
            ),
        )

        fixtures.forEach { (format, entries) ->
            val source = createArchive("source.${format.primaryExtension}", *entries)
            val output = file("${format.name.lowercase()}.pdf")
            val result = assertSuccess(
                OfficeToPdfEngine(context, format).convert(
                    ConversionRequest(source.uri(), output.uri()),
                ),
            )
            assertTrue(result.warnings.any { it.code == "basic-office-layout" })
            assertAscii(output, 0, "%PDF")
        }
    }

    @Test
    fun pdfToPptxAndBackProducesOpenablePackageAndPdf() {
        val source = createPdf("roundtrip-source.pdf", pages = 2)
        val pptx = file("roundtrip.pptx")
        assertSuccess(
            PdfToPptxEngine(context).convert(
                ConversionRequest(source.uri(), pptx.uri(), dpi = 96),
            ),
        )
        assertSignature(pptx, 'P'.code, 'K'.code)

        val pdf = file("roundtrip.pdf")
        val result = assertSuccess(
            PptxToPdfEngine(context).convert(
                ConversionRequest(pptx.uri(), pdf.uri()),
            ),
        )
        assertEquals(2, result.convertedItems)
        assertAscii(pdf, 0, "%PDF")
    }

    private fun createPdf(name: String, pages: Int): File = file(name).also { output ->
        val document = PdfDocument()
        try {
            repeat(pages) { index ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(320, 240, index + 1).create())
                page.canvas.drawColor(if (index % 2 == 0) Color.WHITE else Color.rgb(235, 242, 255))
                page.canvas.drawText(
                    "Converty page ${index + 1}",
                    32f,
                    96f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 24f },
                )
                document.finishPage(page)
            }
            FileOutputStream(output).use(document::writeTo)
        } finally {
            document.close()
        }
    }

    private fun createPng(name: String): File = file(name).also { output ->
        val bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb(20, 120, 220))
            FileOutputStream(output).use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun createBmp(name: String): File = file(name).apply {
        val width = 2
        val height = 2
        val rowStride = 8
        val pixelOffset = 54
        val bytes = ByteArray(pixelOffset + rowStride * height)
        bytes[0] = 'B'.code.toByte()
        bytes[1] = 'M'.code.toByte()
        bytes.putLeInt(2, bytes.size)
        bytes.putLeInt(10, pixelOffset)
        bytes.putLeInt(14, 40)
        bytes.putLeInt(18, width)
        bytes.putLeInt(22, height)
        bytes.putLeShort(26, 1)
        bytes.putLeShort(28, 24)
        val colors = arrayOf(
            intArrayOf(255, 0, 0), intArrayOf(0, 255, 0),
            intArrayOf(0, 0, 255), intArrayOf(255, 255, 255),
        )
        colors.forEachIndexed { index, rgb ->
            val row = index / width
            val column = index % width
            val offset = pixelOffset + row * rowStride + column * 3
            bytes[offset] = rgb[2].toByte()
            bytes[offset + 1] = rgb[1].toByte()
            bytes[offset + 2] = rgb[0].toByte()
        }
        writeBytes(bytes)
    }

    private fun transcode(direction: ConversionDirection, source: File, outputName: String): File {
        val output = file(outputName)
        assertSuccess(
            ImageTranscodeEngine(context, direction).convert(
                ConversionRequest(source.uri(), output.uri(), lossless = true),
            ),
        )
        return output
    }

    private fun copyTestAsset(assetName: String, outputName: String): File = file(outputName).also { output ->
        InstrumentationRegistry.getInstrumentation().context.assets.open(assetName).use { input ->
            output.outputStream().use(input::copyTo)
        }
    }

    private fun createArchive(name: String, vararg entries: Pair<String, String>): File = file(name).also { archive ->
        ZipOutputStream(archive.outputStream().buffered()).use { output ->
            entries.forEach { (path, content) ->
                output.putNextEntry(ZipEntry(path))
                output.write(content.toByteArray(Charsets.UTF_8))
                output.closeEntry()
            }
        }
    }

    private fun assertSuccess(result: ConversionResult): ConversionResult.Success {
        assertTrue("Expected success but was $result", result is ConversionResult.Success)
        return result as ConversionResult.Success
    }

    private fun assertSignature(file: File, vararg expected: Int) {
        val bytes = file.readBytes()
        assertTrue("${file.name} is shorter than its signature", bytes.size >= expected.size)
        expected.forEachIndexed { index, value -> assertEquals(value and 0xFF, bytes[index].toInt() and 0xFF) }
    }

    private fun assertAscii(file: File, offset: Int, expected: String) {
        val bytes = file.readBytes()
        assertTrue(bytes.size >= offset + expected.length)
        assertEquals(expected, String(bytes, offset, expected.length, Charsets.US_ASCII))
    }

    private fun File.uri(): Uri = Uri.fromFile(this)
    private fun file(name: String): File = File(testDirectory, name)

    private fun ByteArray.putLeShort(offset: Int, value: Int) {
        this[offset] = value.toByte()
        this[offset + 1] = (value ushr 8).toByte()
    }

    private fun ByteArray.putLeInt(offset: Int, value: Int) {
        repeat(4) { index -> this[offset + index] = (value ushr (index * 8)).toByte() }
    }
}
