package com.converty.app.core.conversion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class PdfToPptxEngine(private val context: Context) : ConversionEngine {
    override val capabilities = ConversionCapabilities(
        sourceFormat = DocumentFormat.PDF,
        destinationFormat = DocumentFormat.PPTX,
        supportsItemSelection = true,
        preservesEditableText = false,
        notes = setOf("image-only-slides", "page-at-a-time-rendering"),
    )

    override fun inspect(source: android.net.Uri, cancellation: ConversionCancellation): DocumentInspectionResult {
        if (cancellation.isCancellationRequested()) return DocumentInspectionResult.Cancelled
        return try {
            val descriptor = try {
                context.contentResolver.openFileDescriptor(source, "r")
                    ?: return DocumentInspectionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Source PDF cannot be opened")
            } catch (error: SecurityException) {
                return DocumentInspectionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Permission to read source PDF was denied", error)
            }
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (cancellation.isCancellationRequested()) DocumentInspectionResult.Cancelled
                    else if (renderer.pageCount <= 0) DocumentInspectionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, "PDF contains no pages")
                    else DocumentInspectionResult.Success(DocumentInspection(DocumentFormat.PDF, renderer.pageCount))
                }
            }
        } catch (error: SecurityException) {
            DocumentInspectionResult.Failure(ErrorCode.ENCRYPTED_DOCUMENT, "PDF is encrypted or cannot be opened", error)
        } catch (error: IOException) {
            DocumentInspectionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "PDF inspection failed", error)
        } catch (error: RuntimeException) {
            DocumentInspectionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, error.message ?: "Malformed PDF", error)
        }
    }

    override fun convert(
        request: ConversionRequest,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        var temporaryOutput: File? = null
        return try {
            val descriptor = try {
                context.contentResolver.openFileDescriptor(request.source, "r")
                    ?: return ConversionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Source PDF cannot be opened")
            } catch (error: SecurityException) {
                return ConversionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Permission to read source PDF was denied", error)
            }
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) {
                        return ConversionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, "PDF contains no pages")
                    }
                    val selected = normalizeSelection(request.selectedItemIndices, renderer.pageCount)
                        ?: return ConversionResult.Failure(ErrorCode.INVALID_SELECTION, "Page selection is outside the PDF")
                    if (selected.isEmpty()) {
                        return ConversionResult.Failure(ErrorCode.INVALID_SELECTION, "No PDF page was selected")
                    }
                    if (cancellation.isCancellationRequested()) return ConversionResult.Cancelled
                    val firstSize = renderer.openPage(selected.first()).use { EmuSize(it.width * EMU_PER_POINT, it.height * EMU_PER_POINT) }
                    val slideSize = slideSize(request.slideSizePreset, firstSize)
                    val tempFile = File.createTempFile("converty-output-", ".pptx", context.cacheDir)
                    temporaryOutput = tempFile
                    FileOutputStream(tempFile).use { fileOutput ->
                        PptxPackageWriter(fileOutput, slideSize).use { writer ->
                            selected.forEachIndexed { completed, pageIndex ->
                                if (cancellation.isCancellationRequested()) throw ConversionCancelledException()
                                onProgress(ConversionProgress(completed, selected.size, ConversionProgress.Stage.RENDERING))
                                renderer.openPage(pageIndex).use { page ->
                                    val dimensions = renderDimensions(page.width, page.height, request.dpi)
                                    var bitmapToRecycle: Bitmap? = null
                                    try {
                                        val bitmap = Bitmap.createBitmap(dimensions.first, dimensions.second, Bitmap.Config.ARGB_8888)
                                        bitmapToRecycle = bitmap
                                        bitmap.eraseColor(android.graphics.Color.WHITE)
                                        val matrix = Matrix().apply { setScale(dimensions.first.toFloat() / page.width, dimensions.second.toFloat() / page.height) }
                                        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                        val bytes = ByteArrayOutputStream().use { encoded ->
                                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, request.imageQuality.jpegQuality, encoded)) {
                                                throw IOException("PDF page image could not be encoded")
                                            }
                                            encoded.toByteArray()
                                        }
                                        val placement = PlacementCalculator.calculate(slideSize, bitmap.width, bitmap.height, request.imageFit)
                                        writer.addImageSlide(bytes, "image/jpeg", bitmap.width, bitmap.height, placement)
                                    } finally {
                                        bitmapToRecycle?.recycle()
                                    }
                                }
                                onProgress(ConversionProgress(completed + 1, selected.size, ConversionProgress.Stage.PACKAGING))
                            }
                        }
                    }
                    onProgress(ConversionProgress(selected.size, selected.size, ConversionProgress.Stage.WRITING))
                    val bytes = context.copyTemporaryFileToUri(tempFile, request.destination, cancellation)
                    ConversionResult.Success(request.destination, selected.size, bytes)
                }
            }
        } catch (_: ConversionCancelledException) {
            ConversionResult.Cancelled
        } catch (error: DestinationNotWritableException) {
            ConversionResult.Failure(ErrorCode.DESTINATION_NOT_WRITABLE, error.message ?: "Destination cannot be written", error)
        } catch (error: SecurityException) {
            ConversionResult.Failure(ErrorCode.ENCRYPTED_DOCUMENT, "PDF is encrypted or cannot be opened", error)
        } catch (error: OutOfMemoryError) {
            ConversionResult.Failure(ErrorCode.OUT_OF_MEMORY, "The selected PDF page is too large to render", error)
        } catch (error: IOException) {
            ConversionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "PDF conversion failed", error)
        } catch (error: RuntimeException) {
            ConversionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, error.message ?: "Malformed PDF", error)
        } finally {
            temporaryOutput?.delete()
        }
    }

    private fun renderDimensions(width: Int, height: Int, dpi: Int): Pair<Int, Int> {
        var scale = dpi / 72.0
        scale = minOf(scale, MAX_BITMAP_EDGE.toDouble() / width, MAX_BITMAP_EDGE.toDouble() / height)
        val pixels = width.toDouble() * height * scale * scale
        if (pixels > MAX_BITMAP_PIXELS) scale *= kotlin.math.sqrt(MAX_BITMAP_PIXELS / pixels)
        return maxOf(1, (width * scale).roundToInt()) to maxOf(1, (height * scale).roundToInt())
    }

    private fun slideSize(preset: SlideSizePreset, firstPage: EmuSize): EmuSize = when (preset) {
        SlideSizePreset.WIDESCREEN_16_9 -> EmuSize(12_192_000, 6_858_000)
        SlideSizePreset.STANDARD_4_3 -> EmuSize(9_144_000, 6_858_000)
        SlideSizePreset.MATCH_FIRST_SELECTED_PAGE -> firstPage.fitWithinOoxmlLimits()
    }

    private fun EmuSize.fitWithinOoxmlLimits(): EmuSize {
        val down = minOf(1.0, MAX_SLIDE_EMU.toDouble() / width, MAX_SLIDE_EMU.toDouble() / height)
        val up = maxOf(1.0, MIN_SLIDE_EMU.toDouble() / (width * down), MIN_SLIDE_EMU.toDouble() / (height * down))
        return EmuSize((width * down * up).roundToInt().toLong(), (height * down * up).roundToInt().toLong())
    }

    private companion object {
        const val EMU_PER_POINT = 12_700L
        const val MAX_BITMAP_EDGE = 4_096
        const val MAX_BITMAP_PIXELS = 16_000_000.0
        const val MIN_SLIDE_EMU = 914_400L
        const val MAX_SLIDE_EMU = 51_206_400L
    }
}
