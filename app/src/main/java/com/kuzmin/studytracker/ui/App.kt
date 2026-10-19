package com.kuzmin.studytracker.ui

import androidx.activity.compose.BackHandler
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

    BackHandler(enabled = selected != null) { selected = null }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
    ) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(350)) },
                label = "screen",
            ) { id ->
                if (id == null) {
                    HomeScreen(
                        store = store,
                        avScope = this@AnimatedContent,
                        tab = tab,
                        onTabChange = { tab = it },
                        listStates = listStates,
                        scrollToId = scrollToId,
                        onScrolled = { scrollToId = null },
                        onOpen = { selected = it },
                        onAdd = { sheet = SheetRequest.New(tab) },
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
                } else {
                    val subject = store.subject(id)
                    if (subject != null) {
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
                    } else {
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
