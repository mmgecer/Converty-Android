package com.converty.app.core.conversion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.roundToInt

class PdfThumbnailEngine(private val context: Context) : ConversionEngine {
    override val capabilities = ConversionCapabilities(
        sourceFormat = DocumentFormat.PDF,
        destinationFormat = DocumentFormat.THUMBNAIL,
        supportsItemSelection = false,
        preservesEditableText = false,
        notes = setOf("first-page-only", "max-edge-512"),
    )

    override fun inspect(
        source: android.net.Uri,
        cancellation: ConversionCancellation,
    ): DocumentInspectionResult = inspectPdf(context, source, cancellation)

    override fun convert(
        request: ConversionRequest,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        var temporary: File? = null
        return try {
            val descriptor = context.contentResolver.openFileDescriptor(request.source, "r")
                ?: return ConversionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Source PDF cannot be opened")
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) {
                        return ConversionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, "PDF contains no pages")
                    }
                    onProgress(ConversionProgress(0, 1, ConversionProgress.Stage.RENDERING))
                    val temp = File.createTempFile("converty-thumbnail-", ".jpg", context.cacheDir)
                    temporary = temp
                    renderer.openPage(0).use { page ->
                        val scale = minOf(1.0, 512.0 / page.width, 512.0 / page.height)
                        val width = maxOf(1, (page.width * scale).roundToInt())
                        val height = maxOf(1, (page.height * scale).roundToInt())
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        try {
                            bitmap.eraseColor(Color.WHITE)
                            page.render(
                                bitmap,
                                null,
                                Matrix().apply { setScale(width.toFloat() / page.width, height.toFloat() / page.height) },
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                            )
                            val encoded = FileOutputStream(temp).use { output ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, request.imageQuality.jpegQuality, output)
                            }
                            if (!encoded) throw IOException("Thumbnail could not be encoded")
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    onProgress(ConversionProgress(1, 1, ConversionProgress.Stage.WRITING))
                    val bytes = context.copyTemporaryFileToUri(temp, request.destination, cancellation)
                    ConversionResult.Success(request.destination, 1, bytes)
                }
            }
        } catch (_: ConversionCancelledException) {
            ConversionResult.Cancelled
        } catch (error: DestinationNotWritableException) {
            ConversionResult.Failure(ErrorCode.DESTINATION_NOT_WRITABLE, error.message ?: "Destination cannot be written", error)
        } catch (error: SecurityException) {
            ConversionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Permission to read PDF was denied", error)
        } catch (error: OutOfMemoryError) {
            ConversionResult.Failure(ErrorCode.OUT_OF_MEMORY, "PDF page is too large", error)
        } catch (error: IOException) {
            ConversionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "Thumbnail conversion failed", error)
        } catch (error: RuntimeException) {
            ConversionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, error.message ?: "Malformed PDF", error)
        } finally {
            temporary?.delete()
        }
    }
}

class PdfToTiffEngine(private val context: Context) : ConversionEngine {
    override val capabilities = ConversionCapabilities(
        sourceFormat = DocumentFormat.PDF,
        destinationFormat = DocumentFormat.TIFF,
        supportsItemSelection = true,
        preservesEditableText = false,
        notes = setOf("multi-page-tiff", "uncompressed-rgb"),
    )

    override fun inspect(
        source: android.net.Uri,
        cancellation: ConversionCancellation,
    ): DocumentInspectionResult = inspectPdf(context, source, cancellation)

    override fun convert(
        request: ConversionRequest,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        var temporary: File? = null
        return try {
            val descriptor = context.contentResolver.openFileDescriptor(request.source, "r")
                ?: return ConversionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Source PDF cannot be opened")
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val selected = normalizeSelection(request.selectedItemIndices, renderer.pageCount)
                        ?: return ConversionResult.Failure(ErrorCode.INVALID_SELECTION, "Page selection is outside the PDF")
                    if (selected.isEmpty()) {
                        return ConversionResult.Failure(ErrorCode.INVALID_SELECTION, "No PDF page was selected")
                    }
                    val dimensions = selected.map { pageIndex ->
                        renderer.openPage(pageIndex).use { page ->
                            pdfRenderDimensions(page.width, page.height, request.dpi)
                        }
                    }
                    val temp = File.createTempFile("converty-pages-", ".tiff", context.cacheDir)
                    temporary = temp
                    writeTiff(temp, renderer, selected, dimensions, request.dpi, cancellation, onProgress)
                    onProgress(ConversionProgress(selected.size, selected.size, ConversionProgress.Stage.WRITING))
                    val bytes = context.copyTemporaryFileToUri(temp, request.destination, cancellation)
                    ConversionResult.Success(request.destination, selected.size, bytes)
                }
            }
        } catch (_: ConversionCancelledException) {
            ConversionResult.Cancelled
        } catch (error: DestinationNotWritableException) {
            ConversionResult.Failure(ErrorCode.DESTINATION_NOT_WRITABLE, error.message ?: "Destination cannot be written", error)
        } catch (error: SecurityException) {
            ConversionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Permission to read PDF was denied", error)
        } catch (error: OutOfMemoryError) {
            ConversionResult.Failure(ErrorCode.OUT_OF_MEMORY, "PDF page is too large", error)
        } catch (error: IOException) {
            ConversionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "TIFF conversion failed", error)
        } catch (error: RuntimeException) {
            ConversionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, error.message ?: "Malformed PDF", error)
        } finally {
            temporary?.delete()
        }
    }

    private fun writeTiff(
        file: File,
        renderer: PdfRenderer,
        selected: List<Int>,
        dimensions: List<Pair<Int, Int>>,
        dpi: Int,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ) {
        val ifdSize = 2 + ENTRY_COUNT * 12 + 4
        val extrasPerPage = 6 + 8 + 8
        val ifdBase = 8L
        val extrasBase = ifdBase + selected.size.toLong() * ifdSize
        var pixelOffset = extrasBase + selected.size.toLong() * extrasPerPage
        val layouts = dimensions.mapIndexed { index, size ->
            val byteCount = Math.multiplyExact(Math.multiplyExact(size.first.toLong(), size.second.toLong()), 3L)
            if (pixelOffset + byteCount > MAX_TIFF_BYTES) throw IOException("TIFF output exceeds the safe size limit")
            TiffPageLayout(
                width = size.first,
                height = size.second,
                ifdOffset = ifdBase + index.toLong() * ifdSize,
                bitsOffset = extrasBase + index.toLong() * extrasPerPage,
                xResolutionOffset = extrasBase + index.toLong() * extrasPerPage + 6,
                yResolutionOffset = extrasBase + index.toLong() * extrasPerPage + 14,
                pixelOffset = pixelOffset,
                byteCount = byteCount,
            ).also { pixelOffset += byteCount }
        }
        RandomAccessFile(file, "rw").use { output ->
            output.setLength(pixelOffset)
            output.seek(0)
            output.write(byteArrayOf('I'.code.toByte(), 'I'.code.toByte()))
            output.writeShortLe(42)
            output.writeIntLe(ifdBase)
            layouts.forEachIndexed { index, layout ->
                output.seek(layout.ifdOffset)
                output.writeShortLe(ENTRY_COUNT)
                output.writeEntry(256, TYPE_LONG, 1, layout.width.toLong())
                output.writeEntry(257, TYPE_LONG, 1, layout.height.toLong())
                output.writeEntry(258, TYPE_SHORT, 3, layout.bitsOffset)
                output.writeEntry(259, TYPE_SHORT, 1, 1)
                output.writeEntry(262, TYPE_SHORT, 1, 2)
                output.writeEntry(273, TYPE_LONG, 1, layout.pixelOffset)
                output.writeEntry(277, TYPE_SHORT, 1, 3)
                output.writeEntry(278, TYPE_LONG, 1, layout.height.toLong())
                output.writeEntry(279, TYPE_LONG, 1, layout.byteCount)
                output.writeEntry(282, TYPE_RATIONAL, 1, layout.xResolutionOffset)
                output.writeEntry(283, TYPE_RATIONAL, 1, layout.yResolutionOffset)
                output.writeEntry(284, TYPE_SHORT, 1, 1)
                output.writeEntry(296, TYPE_SHORT, 1, 2)
                output.writeIntLe(layouts.getOrNull(index + 1)?.ifdOffset ?: 0)
                output.seek(layout.bitsOffset)
                repeat(3) { output.writeShortLe(8) }
                output.writeIntLe(dpi.toLong())
                output.writeIntLe(1)
                output.writeIntLe(dpi.toLong())
                output.writeIntLe(1)
            }
            selected.forEachIndexed { completed, pageIndex ->
                if (cancellation.isCancellationRequested()) throw ConversionCancelledException()
                val layout = layouts[completed]
                onProgress(ConversionProgress(completed, selected.size, ConversionProgress.Stage.RENDERING))
                renderer.openPage(pageIndex).use { page ->
                    val bitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(
                            bitmap,
                            null,
                            Matrix().apply {
                                setScale(layout.width.toFloat() / page.width, layout.height.toFloat() / page.height)
                            },
                            PdfRenderer.Page.RENDER_MODE_FOR_PRINT,
                        )
                        output.seek(layout.pixelOffset)
                        val colors = IntArray(layout.width)
                        val bytes = ByteArray(layout.width * 3)
                        for (y in 0 until layout.height) {
                            bitmap.getPixels(colors, 0, layout.width, 0, y, layout.width, 1)
                            var byteIndex = 0
                            colors.forEach { color ->
                                bytes[byteIndex++] = Color.red(color).toByte()
                                bytes[byteIndex++] = Color.green(color).toByte()
                                bytes[byteIndex++] = Color.blue(color).toByte()
                            }
                            output.write(bytes)
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
                onProgress(ConversionProgress(completed + 1, selected.size, ConversionProgress.Stage.PACKAGING))
            }
        }
    }

    private data class TiffPageLayout(
        val width: Int,
        val height: Int,
        val ifdOffset: Long,
        val bitsOffset: Long,
        val xResolutionOffset: Long,
        val yResolutionOffset: Long,
        val pixelOffset: Long,
        val byteCount: Long,
    )

    private fun RandomAccessFile.writeEntry(tag: Int, type: Int, count: Int, value: Long) {
        writeShortLe(tag)
        writeShortLe(type)
        writeIntLe(count.toLong())
        if (type == TYPE_SHORT && count == 1) {
            writeShortLe(value.toInt())
            writeShortLe(0)
        } else {
            writeIntLe(value)
        }
    }

    private fun RandomAccessFile.writeShortLe(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    private fun RandomAccessFile.writeIntLe(value: Long) {
        repeat(4) { shift -> write(((value ushr (shift * 8)) and 0xFF).toInt()) }
    }

    private companion object {
        const val ENTRY_COUNT = 13
        const val TYPE_SHORT = 3
        const val TYPE_LONG = 4
        const val TYPE_RATIONAL = 5
        const val MAX_TIFF_BYTES = 512L * 1024 * 1024
    }
}
