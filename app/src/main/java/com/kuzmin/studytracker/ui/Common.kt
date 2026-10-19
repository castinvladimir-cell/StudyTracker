package com.kuzmin.studytracker.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuzmin.studytracker.data.Images
import com.kuzmin.studytracker.ui.theme.Palette
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

const val DAY_MS = 86_400_000L

/** Одинаковая плавная анимация для всех общих элементов при раскрытии карточки. */
@OptIn(ExperimentalSharedTransitionApi::class)
val CardBoundsTransform = BoundsTransform { _, _ -> tween(450, easing = FastOutSlowInEasing) }

@Composable
fun SubjectImage(path: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { path?.let { Images.load(it) } }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        PencilArt(modifier)
    }
}

// ---------- сроки ----------

private val ruLocale: Locale = Locale.forLanguageTag("ru")
private val shortDate = DateTimeFormatter.ofPattern("d MMM", ruLocale)
private val fullDate = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", ruLocale)

fun formatFullDate(day: Long): String = LocalDate.ofEpochDay(day).format(fullDate)

private fun daysWord(n: Long): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1L && m100 != 11L -> "день"
        m10 in 2L..4L && m100 !in 12L..14L -> "дня"
        else -> "дней"
    }
}

/** Текст срока и его цвет: просрочено красным, ближайшие дни жёлтым. */
fun deadlineInfo(day: Long): Pair<String, Color> {
    val diff = day - LocalDate.now().toEpochDay()
    val date = LocalDate.ofEpochDay(day).format(shortDate)
    return when {
        diff < 0 -> "$date · просрочено" to Palette.Danger
        diff == 0L -> "$date · сегодня" to Palette.Warn
        diff == 1L -> "$date · завтра" to Palette.Warn
        diff <= 3 -> "$date · через $diff ${daysWord(diff)}" to Palette.Warn
        else -> "$date · через $diff ${daysWord(diff)}" to Palette.Sub
    }
}

@Composable
fun DeadlineText(
    day: Long,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    fontSize: TextUnit = 13.sp,
) {
    val (text, color) = deadlineInfo(day)
    Text(
        text = "Срок: $text",
        color = if (muted) Palette.Sub else color,
        fontSize = fontSize,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun trackerFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Palette.Text,
    unfocusedTextColor = Palette.Text,
    focusedBorderColor = Palette.Fill,
    unfocusedBorderColor = Palette.CardBorder,
    focusedLabelColor = Palette.Fill,
    unfocusedLabelColor = Palette.Sub,
    cursorColor = Palette.Fill,
    focusedPlaceholderColor = Palette.Sub,
    unfocusedPlaceholderColor = Palette.Sub,
)

// ---------- свайп справа налево, под карточкой кнопки ----------

/**
 * Карточка сдвигается влево пальцем, под ней открываются кнопки (как в eBoox).
 * Открыта может быть только одна: состоянием управляет экран через [open].
 */
@Composable
fun SwipeReveal(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    var actionsWidth by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val currentOpen by rememberUpdatedState(open)
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)
    val settle = spring<Float>(dampingRatio = 0.8f, stiffness = 420f)

    LaunchedEffect(open, actionsWidth) {
        offset.animateTo(if (open) -actionsWidth.toFloat() else 0f, settle)
    }

    Box(modifier) {
        Box(Modifier.matchParentSize()) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .onSizeChanged { actionsWidth = it.width }
                    .graphicsLayer {
                        val k = if (actionsWidth == 0) 0f else (-offset.value / actionsWidth).coerceIn(0f, 1f)
                        alpha = k
                        translationX = (1f - k) * 40f
                    },
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    var lastDx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { lastDx = 0f },
                        onDragEnd = {
                            val w = actionsWidth.toFloat()
                            val shouldOpen = w > 0f &&
                                (offset.value < -w * 0.6f || (offset.value < -w * 0.15f && lastDx < 0f))
                            scope.launch { offset.animateTo(if (shouldOpen) -w else 0f, settle) }
                            if (shouldOpen != currentOpen) currentOnOpenChange(shouldOpen)
                        },
                        onDragCancel = {
                            scope.launch {
                                offset.animateTo(if (currentOpen) -actionsWidth.toFloat() else 0f, settle)
                            }
                        },
                    ) { change, dx ->
                        change.consume()
                        lastDx = dx
                        val w = actionsWidth.toFloat()
                        val next = (offset.value + dx).coerceIn(-w * 1.12f, 0f)
                        scope.launch { offset.snapTo(next) }
                    }
                }
        ) {
            content()
        }
    }
}

@Composable
fun SwipeAction(text: String, color: Color = Palette.Sub, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
fun SwipeActionDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Palette.CardBorder)
    )
}
