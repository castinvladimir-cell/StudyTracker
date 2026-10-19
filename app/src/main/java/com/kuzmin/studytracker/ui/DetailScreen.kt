package com.kuzmin.studytracker.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuzmin.studytracker.data.StudyTask
import com.kuzmin.studytracker.data.Store
import com.kuzmin.studytracker.data.Subject
import com.kuzmin.studytracker.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TaskShape = RoundedCornerShape(6.dp)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DetailScreen(
    subject: Subject,
    store: Store,
    avScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteTask: (Long) -> Unit,
) {
    var editingTask by remember { mutableStateOf<Long?>(null) }
    var openSwipe by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .sharedBounds(
                rememberSharedContentState("card-${subject.id}"),
                avScope,
                boundsTransform = CardBoundsTransform,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(6.dp)),
            )
            .fillMaxSize()
            .background(Palette.Bg)
    ) {
        Column(
            Modifier
                .skipToLookaheadSize()
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Palette.Text)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Изменить предмет", tint = Palette.TextDim)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp + navBottom),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "header") {
                    Header(subject, avScope)
                }
                item(key = "label") {
                    Row(
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Задания", color = Palette.TextDim, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "${subject.tasks.size}",
                            color = Palette.Sub,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                items(subject.tasks, key = { it.id }) { task ->
                    SwipeReveal(
                        open = openSwipe == task.id,
                        onOpenChange = { isOpen ->
                            openSwipe = if (isOpen) task.id else if (openSwipe == task.id) null else openSwipe
                        },
                        modifier = Modifier.animateItem(),
                        actions = {
                            SwipeAction("Изменить") {
                                openSwipe = null
                                editingTask = task.id
                            }
                            SwipeActionDivider()
                            SwipeAction("Удалить", Palette.Danger) {
                                openSwipe = null
                                onDeleteTask(task.id)
                            }
                        },
                    ) {
                        TaskCell(
                            task = task,
                            onToggle = {
                                store.updateTask(subject.id, task.id) { it.copy(done = !it.done) }
                            },
                            onClick = {
                                if (openSwipe != null) openSwipe = null else editingTask = task.id
                            },
                        )
                    }
                }
                item(key = "add") {
                    AddTaskButton(
                        modifier = Modifier.animateItem(),
                        onClick = {
                            openSwipe = null
                            val id = store.addTask(subject.id)
                            editingTask = id
                            scope.launch {
                                delay(60)
                                listState.animateScrollToItem(maxOf(0, listState.layoutInfo.totalItemsCount - 1))
                            }
                        },
                    )
                }
            }
        }
    }

    val taskId = editingTask
    val task = taskId?.let { id -> subject.tasks.firstOrNull { it.id == id } }
    if (task != null) {
        key(task.id) {
            TaskSheet(
                task = task,
                onDismiss = { editingTask = null },
                onSave = { updated -> store.updateTask(subject.id, task.id) { updated } },
                onDelete = {
                    editingTask = null
                    onDeleteTask(task.id)
                },
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.Header(subject: Subject, avScope: AnimatedVisibilityScope) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            SubjectImage(
                path = subject.image,
                modifier = Modifier
                    .sharedElement(
                        rememberSharedContentState("img-${subject.id}"),
                        avScope,
                        boundsTransform = CardBoundsTransform,
                        zIndexInOverlay = 1f,
                    )
                    .size(124.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Column(
                Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = subject.name,
                    color = Palette.Text,
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (subject.teacher.isNotBlank()) {
                    Text(
                        text = subject.teacher,
                        color = Palette.Sub,
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = "Выполнено ${subject.doneCount} из ${subject.tasks.size}",
                    color = Palette.TextDim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                subject.nearestDeadline?.let {
                    DeadlineText(it, Modifier.padding(top = 3.dp), fontSize = 14.sp)
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 18.dp),
        ) {
            ProgressLine(
                progress = subject.progress,
                thickness = 6.dp,
                modifier = Modifier
                    .weight(1f)
                    .sharedElement(
                        rememberSharedContentState("prog-${subject.id}"),
                        avScope,
                        boundsTransform = CardBoundsTransform,
                    ),
            )
            val pct by animateIntAsState((subject.progress * 100).roundToInt(), label = "pct")
            Text(
                text = "$pct%",
                color = Palette.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun TaskCell(task: StudyTask, onToggle: () -> Unit, onClick: () -> Unit) {
    val titleColor by animateColorAsState(if (task.done) Palette.Sub else Palette.Text, label = "title")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TaskShape)
            .background(Palette.Card)
            .border(1.dp, Palette.CardBorder, TaskShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.Top,
    ) {
        CheckCircle(checked = task.done, onToggle = onToggle)
        Column(
            Modifier
                .padding(start = 14.dp, top = 2.dp)
                .weight(1f)
        ) {
            Text(
                text = task.title.ifBlank { "Без названия" },
                color = titleColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (task.note.isNotBlank()) {
                Text(
                    text = task.note,
                    color = Palette.TextDim,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            task.deadline?.let {
                DeadlineText(it, Modifier.padding(top = 6.dp), muted = task.done)
            }
        }
    }
}

@Composable
private fun AddTaskButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TaskShape)
            .drawBehind {
                drawRoundRect(
                    color = Palette.CardBorder,
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Palette.Sub, modifier = Modifier.size(20.dp))
        Text(
            "Добавить задание",
            color = Palette.Sub,
            fontSize = 15.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
