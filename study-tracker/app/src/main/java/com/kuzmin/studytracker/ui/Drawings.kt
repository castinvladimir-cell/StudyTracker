package com.kuzmin.studytracker.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kuzmin.studytracker.ui.theme.Palette

/** Нарисованный карандашик: картинка предмета по умолчанию. */
@Composable
fun PencilArt(modifier: Modifier = Modifier) {
    Canvas(modifier.background(Palette.Surface2)) {
        val s = size.minDimension
        val c = center

        // линия, которую «нарисовал» карандаш
        val line = Path().apply {
            moveTo(c.x - s * 0.245f, c.y + s * 0.27f)
            cubicTo(
                c.x - s * 0.17f, c.y + s * 0.37f,
                c.x - s * 0.06f, c.y + s * 0.20f,
                c.x + s * 0.04f, c.y + s * 0.31f,
            )
            cubicTo(
                c.x + s * 0.12f, c.y + s * 0.40f,
                c.x + s * 0.21f, c.y + s * 0.28f,
                c.x + s * 0.31f, c.y + s * 0.33f,
            )
        }
        drawPath(
            line,
            Palette.Sub.copy(alpha = 0.6f),
            style = Stroke(width = s * 0.024f, cap = StrokeCap.Round),
        )

        rotate(-45f, pivot = c) {
            val len = s * 0.70f
            val h = s * 0.15f
            val left = c.x - len / 2
            val top = c.y - h / 2
            val tip = len * 0.20f
            val band = len * 0.07f
            val eraser = len * 0.12f
            val bodyStart = left + tip
            val bodyEnd = left + len - band - eraser
            val bodyW = bodyEnd - bodyStart

            // ластик
            drawRoundRect(
                Color(0xFFC48A8A),
                topLeft = Offset(bodyEnd + band - h * 0.3f, top),
                size = Size(eraser + h * 0.3f, h),
                cornerRadius = CornerRadius(h * 0.3f),
            )
            // ободок
            drawRect(Color(0xFF6C6C76), Offset(bodyEnd, top), Size(band, h))
            drawLine(
                Color(0xFF52525A),
                Offset(bodyEnd + band * 0.5f, top),
                Offset(bodyEnd + band * 0.5f, top + h),
                strokeWidth = s * 0.008f,
            )
            // корпус в три грани
            drawRect(Color(0xFFB9B9C9), Offset(bodyStart, top), Size(bodyW, h / 3))
            drawRect(Color(0xFFA6A6B8), Offset(bodyStart, top + h / 3), Size(bodyW, h / 3))
            drawRect(Color(0xFF8A8AA0), Offset(bodyStart, top + 2 * h / 3), Size(bodyW, h / 3))
            // дерево
            drawPath(
                Path().apply {
                    moveTo(left, c.y)
                    lineTo(bodyStart, top)
                    lineTo(bodyStart, top + h)
                    close()
                },
                Color(0xFFCDBB9C),
            )
            // грифель
            val g = 0.36f
            drawPath(
                Path().apply {
                    moveTo(left, c.y)
                    lineTo(left + tip * g, c.y - h / 2 * g)
                    lineTo(left + tip * g, c.y + h / 2 * g)
                    close()
                },
                Color(0xFF2C2C31),
            )
        }
    }
}

/** Иконка сортировки как в eBoox: полоски убывающей длины и стрелка вниз. */
@Composable
fun SortIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val u = size.width / 24f
        val w = 1.7f * u
        drawLine(color, Offset(2 * u, 6 * u), Offset(15 * u, 6 * u), w, StrokeCap.Round)
        drawLine(color, Offset(2 * u, 11 * u), Offset(12 * u, 11 * u), w, StrokeCap.Round)
        drawLine(color, Offset(2 * u, 16 * u), Offset(9 * u, 16 * u), w, StrokeCap.Round)
        drawLine(color, Offset(19.5f * u, 5 * u), Offset(19.5f * u, 19 * u), w, StrokeCap.Round)
        drawLine(color, Offset(16.5f * u, 16 * u), Offset(19.5f * u, 19 * u), w, StrokeCap.Round)
        drawLine(color, Offset(22.5f * u, 16 * u), Offset(19.5f * u, 19 * u), w, StrokeCap.Round)
    }
}

/** Полоска прогресса. Изменение значения плавно анимируется. */
@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, thickness: Dp = 3.dp) {
    val p by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progress",
    )
    Canvas(modifier.height(thickness)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(Palette.Track, cornerRadius = r)
        if (p > 0f) {
            drawRoundRect(Palette.Fill, size = Size(size.width * p, size.height), cornerRadius = r)
        }
    }
}

/** Круглая галочка «выполнено» с пружинной анимацией и дорисовкой галки. */
@Composable
fun CheckCircle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "check",
    )
    val interaction = remember { MutableInteractionSource() }
    Canvas(
        modifier
            .size(28.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
    ) {
        val stroke = 2.dp.toPx()
        val r = size.minDimension / 2 - stroke / 2
        val k = t.coerceIn(0f, 1f)

        drawCircle(lerp(Palette.Sub, Palette.Fill, k), radius = r, style = Stroke(stroke))
        if (t > 0.01f) {
            drawCircle(Palette.Fill, radius = r * t.coerceIn(0f, 1.12f))
        }
        if (k > 0.05f) {
            val w = size.width
            val h = size.height
            val check = Path().apply {
                moveTo(w * 0.29f, h * 0.52f)
                lineTo(w * 0.44f, h * 0.67f)
                lineTo(w * 0.72f, h * 0.37f)
            }
            val pm = PathMeasure()
            pm.setPath(check, false)
            val part = Path()
            pm.getSegment(0f, pm.length * k, part, true)
            drawPath(
                part,
                Palette.OnFill,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
