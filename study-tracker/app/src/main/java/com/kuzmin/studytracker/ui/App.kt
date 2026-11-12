package com.kuzmin.studytracker.ui

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.kuzmin.studytracker.data.Backup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kuzmin.studytracker.data.Store
import com.kuzmin.studytracker.data.TAB_CURRENT
import com.kuzmin.studytracker.ui.theme.Palette
import kotlinx.coroutines.launch

private sealed interface SheetRequest {
    data class New(val tab: Int) : SheetRequest
    data class Edit(val id: Long) : SheetRequest
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TrackerApp(store: Store) {
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(TAB_CURRENT) }
    val listStates = listOf(rememberLazyListState(), rememberLazyListState())
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }
    var scrollToId by remember { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun notify(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    // ---------- копия данных ----------

    val context = LocalContext.current
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    fun say(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    fun restore(uri: Uri) {
        scope.launch {
            val list = withContext(Dispatchers.IO) { Backup.read(context, uri) }
            if (list == null) {
                say("Не получилось: это не файл копии")
            } else {
                selected = null
                store.replaceAll(list)
                say("Загружено предметов: ${list.size}")
            }
        }
    }

    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (store.subjects.isEmpty()) restore(uri) else pendingRestore = uri
        }
    }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { Backup.writeTo(context, uri, store.subjects) }
                say(if (ok) "Копия сохранена" else "Не удалось сохранить копию")
            }
        }
    }

    fun backup() {
        if (Build.VERSION.SDK_INT >= 29) {
            scope.launch {
                val name = withContext(Dispatchers.IO) { Backup.saveToDownloads(context, store.subjects) }
                say(if (name != null) "Сохранено в Загрузки/StudyTracker" else "Не удалось сохранить копию")
            }
        } else {
            createBackup.launch(Backup.fileName())
        }
    }

    val restoreUri = pendingRestore
    if (restoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            containerColor = Palette.Sheet,
            titleContentColor = Palette.Text,
            textContentColor = Palette.TextDim,
            title = { Text("Загрузить копию?") },
            text = { Text("Все предметы и задания, которые есть сейчас, заменятся на те, что в файле.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    restore(restoreUri)
                }) { Text("Заменить", color = Palette.Fill) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Отмена", color = Palette.Sub) }
            },
        )
    }

    BackHandler(enabled = selected != null) { selected = null }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
    ) {
        SharedTransitionLayout {
            // Главный экран всегда остаётся на месте, экран предмета открывается поверх него.
            // Так при возврате ничего не строится заново и анимация стартует без паузы.
            HomeScreen(
                        store = store,
                        selectedId = selected,
                        tab = tab,
                        onTabChange = { tab = it },
                        listStates = listStates,
                        scrollToId = scrollToId,
                        onScrolled = { scrollToId = null },
                        onOpen = { selected = it },
                        onAdd = { sheet = SheetRequest.New(tab) },
                        onBackup = { backup() },
                        onRestore = { pickBackup.launch(arrayOf("*/*")) },
                        onEdit = { sheet = SheetRequest.Edit(it) },
                        onMove = { subjectId ->
                            val s = store.subject(subjectId) ?: return@HomeScreen
                            store.moveSubject(subjectId)
                            val where = if (s.tab == TAB_CURRENT) "«Долг»" else "«Текущий предмет»"
                            notify("${s.name}: перенесено в $where") { store.moveSubject(subjectId) }
                        },
                        onDelete = { subjectId ->
                            val removed = store.deleteSubject(subjectId) ?: return@HomeScreen
                            notify("Предмет «${removed.first.name}» удалён") {
                                store.restoreSubject(removed.first, removed.second)
                            }
                        },
            )
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                label = "screen",
            ) { id ->
                Box(Modifier.fillMaxSize()) {
                    val subject = id?.let { store.subject(it) }
                    if (id != null && subject != null) {
                        DetailScreen(
                            subject = subject,
                            store = store,
                            avScope = this@AnimatedContent,
                            onBack = { selected = null },
                            onEdit = { sheet = SheetRequest.Edit(id) },
                            onDeleteTask = { taskId ->
                                val removed = store.deleteTask(id, taskId) ?: return@DetailScreen
                                notify("Задание удалено") { store.restoreTask(id, removed.first, removed.second) }
                            },
                        )
                    } else if (id != null) {
                        LaunchedEffect(Unit) { selected = null }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (selected == null) 92.dp else 12.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Palette.Surface2,
                contentColor = Palette.Text,
                actionColor = Palette.Fill,
            )
        }
    }

    when (val req = sheet) {
        null -> Unit
        is SheetRequest.New -> key(req) {
            SubjectSheet(
                initial = null,
                onDismiss = { sheet = null },
                onSave = { name, teacher, image ->
                    val id = store.addSubject(name, teacher, image, req.tab)
                    sheet = null
                    scrollToId = id
                },
            )
        }
        is SheetRequest.Edit -> key(req) {
            val s = store.subject(req.id)
            if (s == null) {
                LaunchedEffect(Unit) { sheet = null }
            } else {
                SubjectSheet(
                    initial = s,
                    onDismiss = { sheet = null },
                    onSave = { name, teacher, image ->
                        store.updateSubject(s.id) { it.copy(name = name, teacher = teacher, image = image) }
                        sheet = null
                    },
                )
            }
        }
    }
}
