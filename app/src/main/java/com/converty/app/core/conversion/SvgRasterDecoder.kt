package com.converty.app.core.conversion

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Xml
import androidx.core.graphics.PathParser
import java.io.File
import java.io.IOException
import java.util.ArrayDeque
import org.xmlpull.v1.XmlPullParser

internal object SvgRasterDecoder {
    private const val DEFAULT_SIZE = 1024
    private const val MAX_EDGE = 8192
    private const val MAX_PIXELS = 64_000_000L

    fun decode(file: File): DecodedRaster {
        val source = file.readText(Charsets.UTF_8)
        if ("<!DOCTYPE" in source.uppercase()) throw IOException("SVG DOCTYPE is not allowed")
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
            setInput(source.reader())
        }
        while (parser.eventType != XmlPullParser.START_TAG && parser.eventType != XmlPullParser.END_DOCUMENT) {
            parser.next()
        }
        if (parser.name != "svg") throw IOException("SVG root element is missing")
        val viewBox = parseNumbers(parser.attr("viewBox"))
        val viewWidth = viewBox.getOrNull(2)?.takeIf { it > 0f }
        val viewHeight = viewBox.getOrNull(3)?.takeIf { it > 0f }
        var width = parseLength(parser.attr("width")) ?: viewWidth ?: DEFAULT_SIZE.toFloat()
        var height = parseLength(parser.attr("height")) ?: viewHeight ?: DEFAULT_SIZE.toFloat()
        if (width <= 0f || height <= 0f) throw IOException("SVG dimensions are invalid")
        val scale = minOf(1f, MAX_EDGE / width, MAX_EDGE / height)
        width *= scale
        height *= scale
        if (width.toLong() * height.toLong() > MAX_PIXELS) throw IOException("SVG exceeds the safe pixel limit")
        val bitmap = Bitmap.createBitmap(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rootMatrix = Matrix()
        if (viewBox.size >= 4 && viewWidth != null && viewHeight != null) {
            rootMatrix.postTranslate(-viewBox[0], -viewBox[1])
            rootMatrix.postScale(bitmap.width / viewWidth, bitmap.height / viewHeight)
        } else {
            rootMatrix.postScale(scale, scale)
        }
        val rootState = SvgState(matrix = rootMatrix)
        applyStyle(parser, rootState)
        val states = ArrayDeque<SvgState>()
        states.addLast(rootState)
        val unsupported = linkedSetOf<String>()

        var event = parser.next()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.orEmpty()
                    val parent = states.last()
                    val state = parent.copy(
                        matrix = Matrix(parent.matrix),
                        renderEnabled = parent.renderEnabled && tag !in NON_RENDERING_CONTAINERS,
                    )
                    applyStyle(parser, state)
                    applyTransform(parser.attr("transform"), state.matrix)
                    states.addLast(state)
                    if (tag in UNSUPPORTED_FEATURES) unsupported += tag
                    if (state.renderEnabled) {
                        when (tag) {
                            "g" -> Unit
                            "rect" -> drawRect(canvas, parser, state)
                            "circle" -> drawCircle(canvas, parser, state)
                            "ellipse" -> drawEllipse(canvas, parser, state)
                            "line" -> drawLine(canvas, parser, state)
                            "polyline" -> drawPoly(canvas, parser, state, close = false)
                            "polygon" -> drawPoly(canvas, parser, state, close = true)
                            "path" -> drawPath(canvas, parser, state)
                            "svg" -> Unit
                            "title", "desc", "metadata" -> Unit
                            in NON_RENDERING_CONTAINERS, in UNSUPPORTED_FEATURES -> Unit
                            else -> unsupported += tag
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (states.size > 1) states.removeLast()
            }
            event = parser.next()
        }
        return DecodedRaster(
            bitmap,
            unsupported.filter(String::isNotBlank).map { feature ->
                ConversionWarning(
                    code = "svg-unsupported-$feature",
                    message = "SVG feature '$feature' was not rendered",
                )
            },
        )
    }

    private data class SvgState(
        var fill: Int? = Color.BLACK,
        var stroke: Int? = null,
        var strokeWidth: Float = 1f,
        var opacity: Float = 1f,
        var fillEvenOdd: Boolean = false,
        val matrix: Matrix,
        val renderEnabled: Boolean = true,
    )

    private fun applyStyle(parser: XmlPullParser, state: SvgState) {
        val declarations = mutableMapOf<String, String>()
        parser.attr("style")?.split(';')?.forEach { item ->
            val pair = item.split(':', limit = 2)
            if (pair.size == 2) declarations[pair[0].trim()] = pair[1].trim()
        }
        fun value(name: String): String? = parser.attr(name) ?: declarations[name]
        value("fill")?.let { state.fill = parseColor(it) }
        value("stroke")?.let { state.stroke = parseColor(it) }
        value("stroke-width")?.let { parseLength(it)?.let { width -> state.strokeWidth = width } }
        value("opacity")?.toFloatOrNull()?.let { state.opacity = (state.opacity * it).coerceIn(0f, 1f) }
        value("fill-opacity")?.toFloatOrNull()?.let { alpha ->
            state.fill = state.fill?.withAlpha((alpha * state.opacity).coerceIn(0f, 1f))
        }
        value("stroke-opacity")?.toFloatOrNull()?.let { alpha ->
            state.stroke = state.stroke?.withAlpha((alpha * state.opacity).coerceIn(0f, 1f))
        }
        value("fill-rule")?.let { state.fillEvenOdd = it.equals("evenodd", ignoreCase = true) }
    }

    private fun drawRect(canvas: Canvas, parser: XmlPullParser, state: SvgState) {
        val x = parseLength(parser.attr("x")) ?: 0f
        val y = parseLength(parser.attr("y")) ?: 0f
        val width = parseLength(parser.attr("width")) ?: return
        val height = parseLength(parser.attr("height")) ?: return
        val rx = parseLength(parser.attr("rx")) ?: 0f
        val ry = parseLength(parser.attr("ry")) ?: rx
        drawWithState(canvas, state) { fill, stroke ->
            val rect = RectF(x, y, x + width, y + height)
            fill?.let { canvas.drawRoundRect(rect, rx, ry, it) }
            stroke?.let { canvas.drawRoundRect(rect, rx, ry, it) }
        }
    }

    private fun drawCircle(canvas: Canvas, parser: XmlPullParser, state: SvgState) {
        val cx = parseLength(parser.attr("cx")) ?: 0f
        val cy = parseLength(parser.attr("cy")) ?: 0f
        val radius = parseLength(parser.attr("r")) ?: return
        drawWithState(canvas, state) { fill, stroke ->
            fill?.let { canvas.drawCircle(cx, cy, radius, it) }
            stroke?.let { canvas.drawCircle(cx, cy, radius, it) }
        }
    }

    private fun drawEllipse(canvas: Canvas, parser: XmlPullParser, state: SvgState) {
        val cx = parseLength(parser.attr("cx")) ?: 0f
        val cy = parseLength(parser.attr("cy")) ?: 0f
        val rx = parseLength(parser.attr("rx")) ?: return
        val ry = parseLength(parser.attr("ry")) ?: return
        val rect = RectF(cx - rx, cy - ry, cx + rx, cy + ry)
        drawWithState(canvas, state) { fill, stroke ->
            fill?.let { canvas.drawOval(rect, it) }
            stroke?.let { canvas.drawOval(rect, it) }
        }
    }

    private fun drawLine(canvas: Canvas, parser: XmlPullParser, state: SvgState) {
        val x1 = parseLength(parser.attr("x1")) ?: 0f
        val y1 = parseLength(parser.attr("y1")) ?: 0f
        val x2 = parseLength(parser.attr("x2")) ?: 0f
        val y2 = parseLength(parser.attr("y2")) ?: 0f
        drawWithState(canvas, state) { _, stroke -> stroke?.let { canvas.drawLine(x1, y1, x2, y2, it) } }
    }

    private fun drawPoly(canvas: Canvas, parser: XmlPullParser, state: SvgState, close: Boolean) {
        val points = parseNumbers(parser.attr("points"))
        if (points.size < 4) return
        val path = Path().apply {
            moveTo(points[0], points[1])
            var index = 2
            while (index + 1 < points.size) {
                lineTo(points[index], points[index + 1])
                index += 2
            }
            if (close) close()
        }
        drawSvgPath(canvas, path, state)
    }

    private fun drawPath(canvas: Canvas, parser: XmlPullParser, state: SvgState) {
        val pathData = parser.attr("d") ?: return
        val path = runCatching { PathParser.createPathFromPathData(pathData) }.getOrNull() ?: return
        drawSvgPath(canvas, path, state)
    }

    private fun drawSvgPath(canvas: Canvas, path: Path, state: SvgState) {
        path.fillType = if (state.fillEvenOdd) Path.FillType.EVEN_ODD else Path.FillType.WINDING
        drawWithState(canvas, state) { fill, stroke ->
            fill?.let { canvas.drawPath(path, it) }
            stroke?.let { canvas.drawPath(path, it) }
        }
    }

    private inline fun drawWithState(
        canvas: Canvas,
        state: SvgState,
        block: (Paint?, Paint?) -> Unit,
    ) {
        val checkpoint = canvas.save()
        canvas.concat(state.matrix)
        val fill = state.fill?.let { color ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                this.color = color.withAlpha(state.opacity)
            }
        }
        val stroke = state.stroke?.let { color ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = state.strokeWidth
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                this.color = color.withAlpha(state.opacity)
            }
        }
        block(fill, stroke)
        canvas.restoreToCount(checkpoint)
    }

    private fun applyTransform(value: String?, matrix: Matrix) {
        if (value.isNullOrBlank()) return
        TRANSFORM.findAll(value).forEach { match ->
            val name = match.groupValues[1].lowercase()
            val args = parseNumbers(match.groupValues[2])
            when (name) {
                "translate" -> matrix.postTranslate(args.getOrElse(0) { 0f }, args.getOrElse(1) { 0f })
                "scale" -> {
                    val x = args.getOrElse(0) { 1f }
                    matrix.postScale(x, args.getOrElse(1) { x })
                }
                "rotate" -> if (args.size >= 3) matrix.postRotate(args[0], args[1], args[2]) else matrix.postRotate(args.getOrElse(0) { 0f })
                "matrix" -> if (args.size >= 6) {
                    matrix.postConcat(
                        Matrix().apply {
                            setValues(
                                floatArrayOf(
                                    args[0], args[2], args[4],
                                    args[1], args[3], args[5],
                                    0f, 0f, 1f,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    private fun parseNumbers(value: String?): List<Float> = value
        ?.trim()
        ?.split(Regex("[\\s,]+"))
        ?.mapNotNull(String::toFloatOrNull)
        .orEmpty()

    private fun parseLength(value: String?): Float? = value
        ?.trim()
        ?.removeSuffix("px")
        ?.removeSuffix("pt")
        ?.removeSuffix("cm")
        ?.removeSuffix("mm")
        ?.takeUnless { it.endsWith('%') }
        ?.toFloatOrNull()

    private fun parseColor(value: String): Int? {
        val normalized = value.trim().lowercase()
        if (normalized == "none" || normalized.startsWith("url(")) return null
        return runCatching {
            when {
                normalized.matches(Regex("#[0-9a-f]{3}")) -> {
                    val red = normalized[1].digitToInt(16) * 17
                    val green = normalized[2].digitToInt(16) * 17
                    val blue = normalized[3].digitToInt(16) * 17
                    Color.rgb(red, green, blue)
                }
                normalized.startsWith("rgb(") -> {
                    val rgb = parseNumbers(normalized.removePrefix("rgb(").removeSuffix(")"))
                    Color.rgb(rgb[0].toInt(), rgb[1].toInt(), rgb[2].toInt())
                }
                else -> Color.parseColor(normalized)
            }
        }.getOrNull()
    }

    private fun Int.withAlpha(multiplier: Float): Int = Color.argb(
        (Color.alpha(this) * multiplier).toInt().coerceIn(0, 255),
        Color.red(this),
        Color.green(this),
        Color.blue(this),
    )

    private fun XmlPullParser.attr(name: String): String? = getAttributeValue(null, name)
    private val TRANSFORM = Regex("([A-Za-z]+)\\s*\\(([^)]*)\\)")
    private val NON_RENDERING_CONTAINERS = setOf("defs", "clipPath", "mask", "symbol")
    private val UNSUPPORTED_FEATURES = setOf(
        "text",
        "image",
        "use",
        "linearGradient",
        "radialGradient",
        "pattern",
        "filter",
    )
}
