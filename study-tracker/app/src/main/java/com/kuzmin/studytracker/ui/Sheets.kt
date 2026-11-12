package com.kuzmin.studytracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuzmin.studytracker.data.Images
import com.kuzmin.studytracker.data.StudyTask
import com.kuzmin.studytracker.data.Subject
import com.kuzmin.studytracker.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val sheetButtonColors
    @Composable get() = ButtonDefaults.buttonColors(
        containerColor = Palette.Accent,
        contentColor = Color.White,
        disabledContainerColor = Palette.Surface2,
        disabledContentColor = Palette.Sub,
    )

/** Добавление нового предмета (initial == null) или изменение существующего. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSheet(
    initial: Subject?,
    onDismiss: () -> Unit,
    onSave: (name: String, teacher: String, image: String?) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var teacher by remember { mutableStateOf(initial?.teacher ?: "") }
    var image by remember { mutableStateOf(initial?.image) }
    var loading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loading = true
            scope.launch {
                val path = withContext(Dispatchers.IO) { Images.importSquare(context, uri) }
                if (path != null) image = path
                loading = false
            }
        }
    }

    fun hideThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Sheet,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Palette.CardBorder) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = if (initial == null) "Новый предмет" else "Изменить предмет",
                color = Palette.Text,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Palette.CardBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(targetState = image, label = "photo") { path ->
                        SubjectImage(path, Modifier.fillMaxSize())
                    }
                    if (loading) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = Palette.Fill,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(0xCC000000)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = Palette.Text, modifier = Modifier.size(15.dp))
                    }
                }
                Column(
                    Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    Text("Фото предмета", color = Palette.Text, fontSize = 15.sp)
                    Text(
                        "Нажми на картинку и выбери фото, оно обрежется в квадрат",
                        color = Palette.Sub,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    if (image != null) {
                        TextButton(
                            onClick = { image = null },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        ) {
                            Text("Убрать фото", color = Palette.Fill, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название предмета") },
                singleLine = true,
                colors = trackerFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text("Кому сдаю") },
                singleLine = true,
                colors = trackerFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { hideThen(onDismiss) }) {
                    Text("Отмена", color = Palette.Sub)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = name.isNotBlank() && !loading,
                    colors = sheetButtonColors,
                    onClick = { hideThen { onSave(name.trim(), teacher.trim(), image) } },
                ) {
                    Text(if (initial == null) "Добавить" else "Сохранить")
                }
            }
        }
    }
}

/** Редактирование задания. Всё, что изменено, сохраняется и при смахивании окна вниз. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    task: StudyTask,
    onDismiss: () -> Unit,
    onSave: (StudyTask) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(task.title) }
    var note by remember { mutableStateOf(task.note) }
    var deadline by remember { mutableStateOf(task.deadline) }
    var done by remember { mutableStateOf(task.done) }
    var picking by remember { mutableStateOf(false) }

    fun current() = task.copy(title = title.trim(), note = note.trim(), deadline = deadline, done = done)

    ModalBottomSheet(
        onDismissRequest = {
            onSave(current())
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Palette.Sheet,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Palette.CardBorder) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Text("Задание", color = Palette.Text, fontSize = 21.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название") },
                colors = trackerFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Описание") },
                placeholder = { Text("Что нужно сделать, заметки") },
                minLines = 4,
                colors = trackerFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            val shape = RoundedCornerShape(8.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Palette.Card)
                    .border(1.dp, Palette.CardBorder, shape)
                    .clickable { picking = true }
                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = Palette.Sub)
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Text("Срок сдачи", color = Palette.Sub, fontSize = 13.sp)
                    val d = deadline
                    if (d != null) {
                        Text(formatFullDate(d), color = Palette.Text, fontSize = 16.sp)
                        DeadlineText(d, muted = done)
                    } else {
                        Text("Не задан, нажми чтобы выбрать", color = Palette.TextDim, fontSize = 16.sp)
                    }
                }
                if (deadline != null) {
                    IconButton(onClick = { deadline = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Убрать срок", tint = Palette.Sub)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Palette.Card)
                    .border(1.dp, Palette.CardBorder, shape)
                    .clickable { done = !done }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckCircle(checked = done, onToggle = { done = !done })
                Text(
                    "Выполнено",
                    color = Palette.Text,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDelete() }
                }) {
                    Text("Удалить", color = Palette.Danger)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    colors = sheetButtonColors,
                    onClick = {
                        onSave(current())
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                ) {
                    Text("Готово")
                }
            }
        }
    }

    if (picking) {
        DeadlinePicker(
            current = deadline,
            onPick = {
                deadline = it
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlinePicker(current: Long?, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (current ?: LocalDate.now().toEpochDay()) * DAY_MS
    )
    val colors = DatePickerDefaults.colors(
        containerColor = Palette.Sheet,
        selectedDayContainerColor = Palette.Fill,
        selectedDayContentColor = Palette.OnFill,
        todayContentColor = Palette.Fill,
        todayDateBorderColor = Palette.Fill,
        selectedYearContainerColor = Palette.Fill,
        selectedYearContentColor = Palette.OnFill,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms != null) onPick(Math.floorDiv(ms, DAY_MS)) else onDismiss()
            }) {
                Text("Готово", color = Palette.Fill)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = Palette.Sub) }
        },
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false)
    }
}
