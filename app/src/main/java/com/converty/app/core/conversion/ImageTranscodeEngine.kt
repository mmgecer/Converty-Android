package com.converty.app.core.conversion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.FileFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageTranscodeEngine(
    private val context: Context,
    private val direction: ConversionDirection,
) : ConversionEngine {
    init {
        require(direction.sourceFormat in SUPPORTED_SOURCES)
        require(direction.targetFormat in SUPPORTED_TARGETS)
    }

    override val capabilities = ConversionCapabilities(
        sourceFormat = direction.sourceFormat.toEngineFormat(),
        destinationFormat = direction.targetFormat.toEngineFormat(),
        supportsItemSelection = false,
        preservesEditableText = false,
        notes = setOf("single-raster-image", "bounded-memory"),
    )

    override fun inspect(
        source: android.net.Uri,
        cancellation: ConversionCancellation,
    ): DocumentInspectionResult {
        if (cancellation.isCancellationRequested()) return DocumentInspectionResult.Cancelled
        return try {
            context.contentResolver.openInputStream(source)?.use { input ->
                if (input.read() < 0) {
                    return DocumentInspectionResult.Failure(
                        ErrorCode.MALFORMED_DOCUMENT,
                        "Image is empty",
                    )
                }
            } ?: return DocumentInspectionResult.Failure(
                ErrorCode.SOURCE_NOT_READABLE,
                "Source image cannot be opened",
            )
            DocumentInspectionResult.Success(
                DocumentInspection(direction.sourceFormat.toEngineFormat(), 1),
            )
        } catch (error: SecurityException) {
            DocumentInspectionResult.Failure(
                ErrorCode.SOURCE_NOT_READABLE,
                "Permission to read source image was denied",
                error,
            )
        } catch (error: IOException) {
            DocumentInspectionResult.Failure(
                ErrorCode.IO_ERROR,
                error.message ?: "Image inspection failed",
                error,
            )
        }
    }

    override fun convert(
        request: ConversionRequest,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult {
        var sourceFile: File? = null
        var outputFile: File? = null
        var decoded: Bitmap? = null
        var flattened: Bitmap? = null
        var decodeWarnings: List<ConversionWarning> = emptyList()
        return try {
            if (!sourceCodecAvailable()) {
                return ConversionResult.Unsupported(
                    features = setOf("${direction.sourceFormat.name.lowercase()}-decoder"),
                    warnings = emptyList(),
                )
            }
            onProgress(ConversionProgress(0, 1, ConversionProgress.Stage.READING))
            sourceFile = context.copyUriToTemporaryFile(
                request.source,
                ".${direction.sourceExtension}",
                cancellation,
                MAX_SOURCE_BYTES,
            )
            if (cancellation.isCancellationRequested()) return ConversionResult.Cancelled
            val decodedRaster = decode(sourceFile)
                ?: return ConversionResult.Failure(
                    ErrorCode.MALFORMED_DOCUMENT,
                    "The image codec could not decode this file",
                )
            decoded = decodedRaster.bitmap
            decodeWarnings = decodedRaster.warnings
            if (decoded.width <= 0 || decoded.height <= 0 ||
                decoded.width.toLong() * decoded.height > MAX_PIXELS
            ) {
                return ConversionResult.Failure(
                    ErrorCode.SECURITY_LIMIT_EXCEEDED,
                    "Image dimensions exceed the safe processing limit",
                )
            }
            onProgress(ConversionProgress(0, 1, ConversionProgress.Stage.RENDERING))
            val bitmapToEncode = if (direction.targetFormat == FileFormat.JPG && decoded.hasAlpha()) {
                Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888).also { target ->
                    flattened = target
                    Canvas(target).apply {
                        drawColor(Color.WHITE)
                        drawBitmap(decoded, 0f, 0f, null)
                    }
                }
            } else {
                decoded
            }
            outputFile = File.createTempFile(
                "converty-image-",
                ".${direction.targetExtension}",
                context.cacheDir,
            )
            val encoded = FileOutputStream(outputFile).use { output ->
                bitmapToEncode.compress(
                    compressFormat(direction.targetFormat, request.lossless),
                    compressionQuality(direction.targetFormat, request.imageQuality, request.lossless),
                    output,
                )
            }
            if (!encoded) {
                return ConversionResult.Failure(ErrorCode.IO_ERROR, "Image encoder failed")
            }
            onProgress(ConversionProgress(1, 1, ConversionProgress.Stage.WRITING))
            val bytes = context.copyTemporaryFileToUri(outputFile, request.destination, cancellation)
            ConversionResult.Success(request.destination, 1, bytes, decodeWarnings)
        } catch (_: ConversionCancelledException) {
            ConversionResult.Cancelled
        } catch (error: DestinationNotWritableException) {
            ConversionResult.Failure(
                ErrorCode.DESTINATION_NOT_WRITABLE,
                error.message ?: "Destination cannot be written",
                error,
            )
        } catch (error: OutOfMemoryError) {
            ConversionResult.Failure(ErrorCode.OUT_OF_MEMORY, "Image is too large to decode", error)
        } catch (error: IOException) {
            ConversionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "Image conversion failed", error)
        } catch (error: RuntimeException) {
            ConversionResult.Failure(
                ErrorCode.MALFORMED_DOCUMENT,
                error.message ?: "Image conversion failed",
                error,
            )
        } finally {
            if (flattened !== decoded) flattened?.recycle()
            decoded?.recycle()
            sourceFile?.delete()
            outputFile?.delete()
        }
    }

    private fun sourceCodecAvailable(): Boolean = when (direction.sourceFormat) {
        FileFormat.HEIC_HEIF -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        FileFormat.AVIF -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        else -> true
    }

    private fun decode(file: File): DecodedRaster? = when (direction.sourceFormat) {
        FileFormat.BMP -> RasterDecoders.decodeBmp(file)
        FileFormat.TIFF -> RasterDecoders.decodeTiff(file)
        FileFormat.SVG -> SvgRasterDecoder.decode(file)
        else -> {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    val pixels = info.size.width.toLong() * info.size.height
                    if (pixels <= 0 || pixels > MAX_PIXELS) {
                        throw IOException("Image dimensions exceed the safe processing limit")
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val pixels = bounds.outWidth.toLong() * bounds.outHeight
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || pixels > MAX_PIXELS) return null
                BitmapFactory.decodeFile(file.absolutePath)
            }
            bitmap?.let(::DecodedRaster)
        }
    }

    @Suppress("DEPRECATION")
    private fun compressFormat(target: FileFormat, lossless: Boolean): Bitmap.CompressFormat = when (target) {
        FileFormat.PNG -> Bitmap.CompressFormat.PNG
        FileFormat.JPG, FileFormat.THUMBNAIL -> Bitmap.CompressFormat.JPEG
        FileFormat.WEBP -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && lossless -> Bitmap.CompressFormat.WEBP_LOSSLESS
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP_LOSSY
            else -> Bitmap.CompressFormat.WEBP
        }
        else -> error("Unsupported image target $target")
    }

    private fun compressionQuality(
        target: FileFormat,
        quality: ImageQuality,
        lossless: Boolean,
    ): Int = when {
        target == FileFormat.PNG -> 100
        target == FileFormat.WEBP && lossless -> 100
        else -> quality.jpegQuality
    }

    private companion object {
        val SUPPORTED_SOURCES = setOf(
            FileFormat.PNG,
            FileFormat.JPG,
            FileFormat.WEBP,
            FileFormat.TIFF,
            FileFormat.BMP,
            FileFormat.HEIC_HEIF,
            FileFormat.SVG,
            FileFormat.AVIF,
        )
        val SUPPORTED_TARGETS = setOf(FileFormat.PNG, FileFormat.JPG, FileFormat.WEBP)
        const val MAX_SOURCE_BYTES = 256L * 1024 * 1024
        const val MAX_PIXELS = 64_000_000L
    }
}

internal fun FileFormat.toEngineFormat(): DocumentFormat = DocumentFormat.valueOf(name)
