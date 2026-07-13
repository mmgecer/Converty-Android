package com.converty.app.core.conversion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import com.converty.app.core.model.FileFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class PdfToRasterPagesEngine(
    private val context: Context,
    private val target: FileFormat,
) : MultiOutputConversionEngine {
    init {
        require(target == FileFormat.PNG || target == FileFormat.JPG)
    }

    override val capabilities = ConversionCapabilities(
        sourceFormat = DocumentFormat.PDF,
        destinationFormat = target.toEngineFormat(),
        supportsItemSelection = true,
        preservesEditableText = false,
        notes = setOf("one-output-per-page", "page-at-a-time-rendering"),
    )

    override fun inspect(
        source: android.net.Uri,
        cancellation: ConversionCancellation,
    ): DocumentInspectionResult = inspectPdf(context, source, cancellation)

    override fun convertMultiple(
        request: MultiOutputConversionRequest,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ): MultiOutputConversionResult {
        val temporaryFiles = mutableListOf<File>()
        return try {
            val descriptor = context.contentResolver.openFileDescriptor(request.source, "r")
                ?: return MultiOutputConversionResult.Failure(
                    ErrorCode.SOURCE_NOT_READABLE,
                    "Source PDF cannot be opened",
                )
            descriptor.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (request.destinations.any { it.itemIndex !in 0 until renderer.pageCount }) {
                        return MultiOutputConversionResult.Failure(
                            ErrorCode.INVALID_SELECTION,
                            "Page selection is outside the PDF",
                        )
                    }
                    val produced = mutableListOf<ProducedOutput>()
                    request.destinations.forEachIndexed { completed, destination ->
                        if (cancellation.isCancellationRequested()) throw ConversionCancelledException()
                        onProgress(
                            ConversionProgress(
                                completed,
                                request.destinations.size,
                                ConversionProgress.Stage.RENDERING,
                            ),
                        )
                        val temp = File.createTempFile("converty-page-", ".${target.primaryExtension}", context.cacheDir)
                        temporaryFiles += temp
                        renderer.openPage(destination.itemIndex).use { page ->
                            val dimensions = pdfRenderDimensions(page.width, page.height, request.dpi)
                            val bitmap = Bitmap.createBitmap(dimensions.first, dimensions.second, Bitmap.Config.ARGB_8888)
                            try {
                                bitmap.eraseColor(Color.WHITE)
                                val matrix = Matrix().apply {
                                    setScale(
                                        dimensions.first.toFloat() / page.width,
                                        dimensions.second.toFloat() / page.height,
                                    )
                                }
                                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                val encoded = FileOutputStream(temp).use { output ->
                                    bitmap.compress(
                                        if (target == FileFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                                        if (target == FileFormat.PNG) 100 else request.imageQuality.jpegQuality,
                                        output,
                                    )
                                }
                                if (!encoded) throw IOException("Rendered PDF page could not be encoded")
                            } finally {
                                bitmap.recycle()
                            }
                        }
                        val bytes = context.copyTemporaryFileToUri(temp, destination.uri, cancellation)
                        produced += ProducedOutput(destination.itemIndex, destination.uri, bytes)
                        onProgress(
                            ConversionProgress(
                                completed + 1,
                                request.destinations.size,
                                ConversionProgress.Stage.WRITING,
                            ),
                        )
                    }
                    MultiOutputConversionResult.Success(produced)
                }
            }
        } catch (_: ConversionCancelledException) {
            MultiOutputConversionResult.Cancelled
        } catch (error: SecurityException) {
            MultiOutputConversionResult.Failure(
                ErrorCode.SOURCE_NOT_READABLE,
                "Permission to read source PDF was denied",
                error,
            )
        } catch (error: OutOfMemoryError) {
            MultiOutputConversionResult.Failure(ErrorCode.OUT_OF_MEMORY, "PDF page is too large to render", error)
        } catch (error: DestinationNotWritableException) {
            MultiOutputConversionResult.Failure(
                ErrorCode.DESTINATION_NOT_WRITABLE,
                error.message ?: "Destination cannot be written",
                error,
            )
        } catch (error: IOException) {
            MultiOutputConversionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "PDF rendering failed", error)
        } catch (error: RuntimeException) {
            MultiOutputConversionResult.Failure(
                ErrorCode.MALFORMED_DOCUMENT,
                error.message ?: "Malformed PDF",
                error,
            )
        } finally {
            temporaryFiles.forEach(File::delete)
        }
    }
}

internal fun inspectPdf(
    context: Context,
    source: android.net.Uri,
    cancellation: ConversionCancellation,
): DocumentInspectionResult {
    if (cancellation.isCancellationRequested()) return DocumentInspectionResult.Cancelled
    return try {
        val descriptor = context.contentResolver.openFileDescriptor(source, "r")
            ?: return DocumentInspectionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Source PDF cannot be opened")
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount <= 0) {
                    DocumentInspectionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, "PDF contains no pages")
                } else {
                    DocumentInspectionResult.Success(DocumentInspection(DocumentFormat.PDF, renderer.pageCount))
                }
            }
        }
    } catch (error: SecurityException) {
        DocumentInspectionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "Permission to read PDF was denied", error)
    } catch (error: IOException) {
        DocumentInspectionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "PDF inspection failed", error)
    } catch (error: RuntimeException) {
        DocumentInspectionResult.Failure(ErrorCode.MALFORMED_DOCUMENT, error.message ?: "Malformed PDF", error)
    }
}

internal fun pdfRenderDimensions(width: Int, height: Int, dpi: Int): Pair<Int, Int> {
    var scale = dpi / 72.0
    scale = minOf(scale, 8192.0 / width, 8192.0 / height)
    val pixels = width.toDouble() * height * scale * scale
    if (pixels > 64_000_000.0) scale *= kotlin.math.sqrt(64_000_000.0 / pixels)
    return maxOf(1, (width * scale).roundToInt()) to maxOf(1, (height * scale).roundToInt())
}
