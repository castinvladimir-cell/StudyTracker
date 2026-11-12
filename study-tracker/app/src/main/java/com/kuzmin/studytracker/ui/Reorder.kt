package com.kuzmin.studytracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Перетаскивание элементов списка после долгого нажатия.
 * Двигать можно только элементы с ключом типа Long (задания), шапка и кнопки не участвуют.
 */
class ReorderState internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
    private val onStart: () -> Unit,
) {
    /** Индекс элемента, который сейчас тащат (индекс в LazyColumn). */
    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    /** Индекс элемента, который отпустили и он доезжает на место. */
    var settlingIndex by mutableStateOf<Int?>(null)
        private set

    val settleOffset = Animatable(0f)

    internal val scrollChannel = Channel<Float>(Channel.CONFLATED)

    private var draggedDelta by mutableFloatStateOf(0f)
    private var initialOffset by mutableIntStateOf(0)

    private val draggingInfo: LazyListItemInfo?
        get() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingIndex }

    /** На сколько пикселей нарисовать перетаскиваемый элемент относительно его места в списке. */
    val draggingOffset: Float
        get() = draggingInfo?.let { initialOffset + draggedDelta - it.offset } ?: 0f

    internal fun onDragStart(position: Offset) {
        val y = position.y.toInt() + listState.layoutInfo.viewportStartOffset
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key is Long && y in it.offset..(it.offset + it.size)
        } ?: return
        draggingIndex = item.index
        initialOffset = item.offset
        draggedDelta = 0f
        onStart()
    }

    internal fun onDrag(dy: Float) {
        draggedDelta += dy
        val dragging = draggingInfo ?: return
        val start = dragging.offset + draggingOffset
        val end = start + dragging.size
        val middle = (start + (end - start) / 2f).toInt()

        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key is Long && it.index != dragging.index && middle in it.offset..(it.offset + it.size)
        }
        if (target != null) {
            // иначе список «уедет» вслед за первым видимым элементом
            if (dragging.index == listState.firstVisibleItemIndex || target.index == listState.firstVisibleItemIndex) {
                listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
            onMove(dragging.index, target.index)
            draggingIndex = target.index
        } else {
            // у края экрана список прокручивается сам
            val overscroll = when {
                draggedDelta > 0 -> (end - listState.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggedDelta < 0 -> (start - listState.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) scrollChannel.trySend(overscroll)
        }
    }

    internal fun onDragEnd() {
        val index = draggingIndex
        if (index != null) {
            val from = draggingOffset
            settlingIndex = index
            scope.launch {
                settleOffset.snapTo(from)
                settleOffset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                settlingIndex = null
            }
        }
        draggingIndex = null
        draggedDelta = 0f
        initialOffset = 0
    }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    onStart: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
): ReorderState {
    val scope = rememberCoroutineScope()
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnStart by rememberUpdatedState(onStart)
    val state = remember(listState) {
        ReorderState(
            listState = listState,
            scope = scope,
            onMove = { from, to -> currentOnMove(from, to) },
            onStart = { currentOnStart() },
        )
    }
    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            listState.scrollBy(diff)
        }
    }
    return state
}

fun Modifier.reorderContainer(state: ReorderState): Modifier = pointerInput(state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { state.onDragStart(it) },
        onDrag = { change, amount ->
            if (state.draggingIndex != null) {
                change.consume()
                state.onDrag(amount.y)
            }
        },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() },
    )
}
