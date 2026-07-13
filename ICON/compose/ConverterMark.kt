package com.example.converter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Runtime Material 3 version of the converter mark.
 *
 * Every layer reads a semantic MaterialTheme color role, so it follows
 * dynamicLightColorScheme()/dynamicDarkColorScheme() automatically when those
 * schemes are installed by the app theme.
 */
@Composable
fun ConverterMark(
    modifier: Modifier = Modifier,
    includeContainer: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val unit = size.minDimension / 108f

        withTransform({
            scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero)
        }) {
            if (includeContainer) {
                drawRoundRect(
                    color = colors.surfaceContainerHighest,
                    topLeft = Offset.Zero,
                    size = Size(108f, 108f),
                    cornerRadius = CornerRadius(27f, 27f),
                )
            }

            val primaryBlob = Path().apply {
                moveTo(18f, 32f)
                cubicTo(23f, 18f, 41f, 14f, 55f, 18f)
                cubicTo(69f, 12f, 91f, 22f, 92f, 40f)
                cubicTo(99f, 54f, 88f, 67f, 92f, 82f)
                cubicTo(81f, 94f, 63f, 91f, 51f, 94f)
                cubicTo(37f, 91f, 17f, 96f, 14f, 78f)
                cubicTo(8f, 64f, 18f, 50f, 18f, 32f)
                close()
            }
            drawPath(primaryBlob, colors.primaryContainer.copy(alpha = 0.62f))

            val tertiaryBlob = Path().apply {
                moveTo(64f, 17f)
                cubicTo(78f, 15f, 95f, 27f, 94f, 43f)
                cubicTo(100f, 58f, 91f, 74f, 95f, 86f)
                cubicTo(83f, 95f, 69f, 92f, 59f, 88f)
                cubicTo(67f, 76f, 67f, 59f, 61f, 48f)
                cubicTo(56f, 37f, 58f, 25f, 64f, 17f)
                close()
            }
            drawPath(tertiaryBlob, colors.tertiaryContainer.copy(alpha = 0.22f))

            val document = Path().apply {
                moveTo(30f, 20f)
                lineTo(68f, 20f)
                lineTo(86f, 38f)
                lineTo(86f, 80f)
                cubicTo(86f, 85f, 83f, 88f, 78f, 88f)
                lineTo(30f, 88f)
                cubicTo(25f, 88f, 22f, 85f, 22f, 80f)
                lineTo(22f, 28f)
                cubicTo(22f, 23f, 25f, 20f, 30f, 20f)
                close()
            }
            drawPath(document, colors.surfaceContainerLowest)

            clipPath(document) {
                val rightField = Path().apply {
                    moveTo(56f, 18f)
                    lineTo(90f, 18f)
                    lineTo(90f, 91f)
                    lineTo(48f, 91f)
                    cubicTo(57f, 79f, 63f, 67f, 57f, 56f)
                    cubicTo(51f, 45f, 64f, 35f, 56f, 18f)
                    close()
                }
                drawPath(rightField, colors.secondaryContainer)

                val rightGlow = Path().apply {
                    moveTo(60f, 19f)
                    lineTo(92f, 19f)
                    lineTo(92f, 91f)
                    lineTo(54f, 91f)
                    cubicTo(61f, 78f, 66f, 66f, 60f, 55f)
                    cubicTo(54f, 44f, 66f, 34f, 60f, 19f)
                    close()
                }
                drawPath(rightGlow, colors.tertiaryContainer.copy(alpha = 0.42f))
            }

            val fold = Path().apply {
                moveTo(68f, 20f)
                lineTo(68f, 31f)
                cubicTo(68f, 35f, 71f, 38f, 75f, 38f)
                lineTo(86f, 38f)
                close()
            }
            drawPath(fold, colors.primaryContainer)

            fun line(x: Float, y: Float, width: Float) {
                drawRoundRect(
                    color = colors.onSurfaceVariant,
                    topLeft = Offset(x, y),
                    size = Size(width, 4.5f),
                    cornerRadius = CornerRadius(2.25f, 2.25f),
                )
            }
            line(30f, 39f, 24f)
            line(30f, 51f, 21f)
            line(30f, 63f, 16f)

            fun tile(x: Float, y: Float, usePrimary: Boolean) {
                drawRoundRect(
                    color = if (usePrimary) colors.primary else colors.tertiary,
                    topLeft = Offset(x, y),
                    size = Size(10f, 10f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
            }
            tile(63f, 42f, true)
            tile(76f, 42f, false)
            tile(63f, 57f, false)
            tile(76f, 57f, true)

            val seam = Path().apply {
                moveTo(57f, 27f)
                cubicTo(63f, 36f, 52f, 45f, 58f, 55f)
                cubicTo(64f, 65f, 59f, 75f, 51f, 86f)
            }
            drawPath(
                path = seam,
                color = colors.primary,
                style = Stroke(width = 5.5f, cap = StrokeCap.Round),
            )

            val seamAccent = Path().apply {
                moveTo(58.7f, 55f)
                cubicTo(64.3f, 64f, 60f, 73f, 54f, 81f)
            }
            drawPath(
                path = seamAccent,
                color = colors.tertiary,
                style = Stroke(width = 2.2f, cap = StrokeCap.Round),
            )
        }
    }
}
