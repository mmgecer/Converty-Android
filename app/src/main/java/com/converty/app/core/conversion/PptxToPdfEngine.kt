package com.converty.app.core.conversion

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt

class PptxToPdfEngine(private val context: Context) : ConversionEngine {
    override val capabilities = ConversionCapabilities(
        sourceFormat = DocumentFormat.PPTX,
        destinationFormat = DocumentFormat.PDF,
        supportsItemSelection = true,
        preservesEditableText = false,
        notes = setOf("pictures-basic-shapes-and-text", "unsupported-content-is-reported"),
    )

    override fun inspect(source: android.net.Uri, cancellation: ConversionCancellation): DocumentInspectionResult {
        var sourceFile: File? = null
        return try {
            if (cancellation.isCancellationRequested()) return DocumentInspectionResult.Cancelled
            val tempSource = context.copyUriToTemporaryFile(source, ".pptx", cancellation, MAX_SOURCE_BYTES)
            sourceFile = tempSource
            when (val parsed = PptxPackageParser.open(tempSource)) {
                is PptxParseResult.Failure -> parsed.toInspectionFailure()
                is PptxParseResult.Success -> parsed.presentation.use { presentation ->
                    if (cancellation.isCancellationRequested()) {
                        DocumentInspectionResult.Cancelled
                    } else {
                        DocumentInspectionResult.Success(
                            DocumentInspection(DocumentFormat.PPTX, presentation.slides.size, presentation.slides.toWarnings()),
                        )
                    }
                }
            }
        } catch (_: ConversionCancelledException) {
            DocumentInspectionResult.Cancelled
        } catch (error: SourceNotReadableException) {
            DocumentInspectionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, error.message ?: "PPTX cannot be read", error)
        } catch (error: SourceLimitExceededException) {
            DocumentInspectionResult.Failure(ErrorCode.SECURITY_LIMIT_EXCEEDED, error.message ?: "PPTX exceeds a safety limit", error)
        } catch (error: OutOfMemoryError) {
            DocumentInspectionResult.Failure(ErrorCode.OUT_OF_MEMORY, "PPTX is too large to inspect", error)
        } catch (error: IOException) {
            DocumentInspectionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "PPTX inspection failed", error)
        } finally {
            sourceFile?.delete()
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
            onProgress(ConversionProgress(0, 0, ConversionProgress.Stage.READING))
            val tempSource = context.copyUriToTemporaryFile(request.source, ".pptx", cancellation, MAX_SOURCE_BYTES)
            sourceFile = tempSource
            when (val parsed = PptxPackageParser.open(tempSource)) {
                is PptxParseResult.Failure -> parseFailure(parsed)
                is PptxParseResult.Success -> parsed.presentation.use { presentation ->
                    val selected = normalizeSelection(request.selectedItemIndices, presentation.slides.size)
                        ?: return ConversionResult.Failure(ErrorCode.INVALID_SELECTION, "Slide selection is outside the presentation")
                    if (selected.isEmpty()) return ConversionResult.Failure(ErrorCode.INVALID_SELECTION, "No slide was selected")
                    val selectedSlides = selected.map { presentation.slides[it] }
                    val warnings = selectedSlides.toWarnings()
                    val unsupportedSlides = selectedSlides.filterNot { it.isSupported }
                    if (unsupportedSlides.isNotEmpty()) {
                        return ConversionResult.Unsupported(warnings.map { it.message }.toSet(), warnings)
                    }
                    val tempOutput = File.createTempFile("converty-output-", ".pdf", context.cacheDir)
                    outputFile = tempOutput
                    val pdf = PdfDocument()
                    try {
                        val pageWidth = maxOf(1, (presentation.slideSize.width / EMU_PER_POINT.toDouble()).roundToInt())
                        val pageHeight = maxOf(1, (presentation.slideSize.height / EMU_PER_POINT.toDouble()).roundToInt())
                        selected.forEachIndexed { completed, slideIndex ->
                            if (cancellation.isCancellationRequested()) throw ConversionCancelledException()
                            onProgress(ConversionProgress(completed, selected.size, ConversionProgress.Stage.RENDERING))
                            val slide = presentation.slides[slideIndex]
                            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, completed + 1).create()
                            val page = pdf.startPage(pageInfo)
                            try {
                                page.canvas.drawColor(Color.WHITE)
                                renderSlide(page.canvas, presentation, slide)
                            } finally {
                                pdf.finishPage(page)
                            }
                            onProgress(ConversionProgress(completed + 1, selected.size, ConversionProgress.Stage.RENDERING))
                        }
                        FileOutputStream(tempOutput).use { pdf.writeTo(it) }
                    } finally {
                        pdf.close()
                    }
                    onProgress(ConversionProgress(selected.size, selected.size, ConversionProgress.Stage.WRITING))
                    val bytes = context.copyTemporaryFileToUri(tempOutput, request.destination, cancellation)
                    ConversionResult.Success(request.destination, selected.size, bytes, warnings)
                }
            }
        } catch (_: ConversionCancelledException) {
            ConversionResult.Cancelled
        } catch (error: SourceNotReadableException) {
            ConversionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, error.message ?: "PPTX cannot be read", error)
        } catch (error: DestinationNotWritableException) {
            ConversionResult.Failure(ErrorCode.DESTINATION_NOT_WRITABLE, error.message ?: "Destination cannot be written", error)
        } catch (error: SourceLimitExceededException) {
            ConversionResult.Failure(ErrorCode.SECURITY_LIMIT_EXCEEDED, error.message ?: "PPTX exceeds a safety limit", error)
        } catch (error: OutOfMemoryError) {
            ConversionResult.Failure(ErrorCode.OUT_OF_MEMORY, "A slide image is too large to render", error)
        } catch (error: SecurityException) {
            ConversionResult.Failure(ErrorCode.SOURCE_NOT_READABLE, "PPTX cannot be opened", error)
        } catch (error: IOException) {
            ConversionResult.Failure(ErrorCode.IO_ERROR, error.message ?: "PPTX conversion failed", error)
        } finally {
            sourceFile?.delete()
            outputFile?.delete()
        }
    }

    private fun List<ParsedPptxSlide>.toWarnings(): List<ConversionWarning> = flatMap { slide ->
        slide.unsupportedFeatures.map { feature ->
            ConversionWarning("unsupported_pptx_feature", feature, slide.index)
        }
    }

    private fun renderSlide(canvas: Canvas, presentation: ParsedPptx, slide: ParsedPptxSlide) {
        slide.elements.forEach { element ->
            when (element) {
                is PptxSlideElement.Picture -> drawPicture(canvas, presentation, element)
                is PptxSlideElement.Shape -> drawShape(canvas, element)
            }
        }
    }

    private fun drawPicture(canvas: Canvas, presentation: ParsedPptx, picture: PptxSlideElement.Picture) {
        val imageBytes = presentation.readImage(picture)
        val decodeBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeBounds)
        if (decodeBounds.outWidth <= 0 || decodeBounds.outHeight <= 0) throw IOException("Slide image cannot be decoded")
        if (decodeBounds.outWidth.toLong() * decodeBounds.outHeight.toLong() > MAX_DECODED_PIXELS) {
            throw SourceLimitExceededException("Slide image dimensions exceed the safe limit")
        }
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IOException("Slide image cannot be decoded")
        try {
            canvas.withTransform(picture.transform) { bounds ->
                drawBitmap(bitmap, null, bounds, IMAGE_PAINT)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private inline fun Canvas.withTransform(
        transform: PptxTransform,
        clipContent: Boolean = true,
        draw: Canvas.(RectF) -> Unit,
    ) {
        val bounds = transform.bounds.toPoints()
        val saveCount = save()
        try {
            rotate(transform.rotationDegrees, bounds.centerX(), bounds.centerY())
            scale(
                if (transform.flipHorizontally) -1f else 1f,
                if (transform.flipVertically) -1f else 1f,
                bounds.centerX(),
                bounds.centerY(),
            )
            if (clipContent) clipRect(bounds)
            draw(bounds)
        } finally {
            restoreToCount(saveCount)
        }
    }

    private fun EmuRect.toPoints(): RectF = RectF(
        x / EMU_PER_POINT,
        y / EMU_PER_POINT,
        (x + width) / EMU_PER_POINT,
        (y + height) / EMU_PER_POINT,
    )

    private fun drawShape(canvas: Canvas, shape: PptxSlideElement.Shape) {
        canvas.withTransform(shape.transform, clipContent = shape.geometry != PptxPresetGeometry.LINE) { bounds ->
            if (shape.geometry != PptxPresetGeometry.LINE) {
                (shape.fill as? PptxFill.Solid)?.let { fill ->
                    drawGeometry(bounds, shape.geometry, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = fill.argb
                    })
                }
            }
            shape.line?.let { line ->
                (line.fill as? PptxFill.Solid)?.let { lineFill ->
                    drawGeometry(bounds, shape.geometry, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (line.widthEmu / EMU_PER_POINT).coerceAtLeast(MIN_STROKE_POINTS)
                        strokeCap = Paint.Cap.SQUARE
                        color = lineFill.argb
                    })
                }
            }
            shape.textFrame?.let { drawTextFrame(bounds, it) }
        }
    }

    private fun Canvas.drawGeometry(bounds: RectF, geometry: PptxPresetGeometry, paint: Paint) {
        when (geometry) {
            PptxPresetGeometry.RECT -> drawRect(bounds, paint)
            PptxPresetGeometry.ROUND_RECT -> {
                val radius = min(bounds.width(), bounds.height()) * ROUND_RECT_RADIUS_RATIO
                drawRoundRect(bounds, radius, radius, paint)
            }
            PptxPresetGeometry.ELLIPSE -> drawOval(bounds, paint)
            PptxPresetGeometry.LINE -> drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, paint)
        }
    }

    private fun Canvas.drawTextFrame(shapeBounds: RectF, frame: PptxTextFrame) {
        val left = shapeBounds.left + frame.insetLeftEmu / EMU_PER_POINT
        val top = shapeBounds.top + frame.insetTopEmu / EMU_PER_POINT
        val right = shapeBounds.right - frame.insetRightEmu / EMU_PER_POINT
        val bottom = shapeBounds.bottom - frame.insetBottomEmu / EMU_PER_POINT
        val width = (right - left).roundToInt()
        if (width <= 0 || bottom <= top) return
        var y = top
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = DEFAULT_FONT_POINTS
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        frame.paragraphs.forEach { paragraph ->
            if (y >= bottom) return@forEach
            val text = paragraph.toSpannable()
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
                .setAlignment(paragraph.alignment.toLayoutAlignment())
                .setIncludePad(false)
                .setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .also { builder ->
                    if (paragraph.alignment == PptxParagraphAlignment.JUSTIFY && Build.VERSION.SDK_INT >= 26) {
                        builder.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD)
                    }
                }
                .build()
            val saveCount = save()
            try {
                clipRect(left, y, right, bottom)
                translate(left, y)
                layout.draw(this)
            } finally {
                restoreToCount(saveCount)
            }
            y += layout.height
        }
    }

    private fun PptxTextParagraph.toSpannable(): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        runs.forEach { run ->
            val start = builder.length
            builder.append(run.text)
            val end = builder.length
            if (end <= start) return@forEach
            builder.setSpan(ForegroundColorSpan(run.argb), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(
                AbsoluteSizeSpan(run.fontSizePoints.roundToInt().coerceAtLeast(1), false),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            builder.setSpan(TypefaceSpan(run.fontFamily), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            val style = when {
                run.bold && run.italic -> Typeface.BOLD_ITALIC
                run.bold -> Typeface.BOLD
                run.italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            if (style != Typeface.NORMAL) {
                builder.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        if (builder.isEmpty()) builder.append(" ")
        return builder
    }

    private fun PptxParagraphAlignment.toLayoutAlignment(): Layout.Alignment = when (this) {
        PptxParagraphAlignment.LEFT, PptxParagraphAlignment.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
        PptxParagraphAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
        PptxParagraphAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    }

    private fun parseFailure(failure: PptxParseResult.Failure): ConversionResult.Failure {
        val code = when (failure.code) {
            PptxParseFailureCode.SECURITY_LIMIT -> ErrorCode.SECURITY_LIMIT_EXCEEDED
            PptxParseFailureCode.MALFORMED -> ErrorCode.MALFORMED_DOCUMENT
            PptxParseFailureCode.UNSUPPORTED_CONTAINER -> ErrorCode.MALFORMED_DOCUMENT
        }
        return ConversionResult.Failure(code, failure.message, failure.cause)
    }

    private fun PptxParseResult.Failure.toInspectionFailure(): DocumentInspectionResult.Failure {
        val code = when (this.code) {
            PptxParseFailureCode.SECURITY_LIMIT -> ErrorCode.SECURITY_LIMIT_EXCEEDED
            PptxParseFailureCode.MALFORMED,
            PptxParseFailureCode.UNSUPPORTED_CONTAINER -> ErrorCode.MALFORMED_DOCUMENT
        }
        return DocumentInspectionResult.Failure(code, message, cause)
    }

    private companion object {
        const val EMU_PER_POINT = 12_700f
        const val MAX_SOURCE_BYTES = 256L * 1024 * 1024
        const val MAX_DECODED_PIXELS = 64L * 1024 * 1024
        const val DEFAULT_FONT_POINTS = 18f
        const val MIN_STROKE_POINTS = 0.25f
        const val ROUND_RECT_RADIUS_RATIO = 0.12f
        val IMAGE_PAINT = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
}
