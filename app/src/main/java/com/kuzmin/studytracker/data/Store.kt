package com.kuzmin.studytracker.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Всё состояние приложения. Хранится в одном JSON-файле во внутренней памяти,
 * сохраняется с небольшой задержкой после каждого изменения и сразу при сворачивании.
 */
class Store private constructor(context: Context) {

    private val dir: File = context.filesDir
    private val file = File(dir, "subjects.json")
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var saveJob: Job? = null
    private var version = 0L
    private var writtenVersion = -1L
    private var lastId = System.currentTimeMillis()

    var subjects: List<Subject> by mutableStateOf(readFile())
        private set

    var sortMode: SortMode by mutableStateOf(
        SortMode.entries.getOrElse(prefs.getInt("sort", 0)) { SortMode.ADDED }
    )
        private set

    var progressDescending: Boolean by mutableStateOf(prefs.getBoolean("progress_desc", true))
        private set

    init {
        val maxId = subjects.flatMap { s -> listOf(s.id) + s.tasks.map { it.id } }.maxOrNull() ?: 0L
        lastId = maxOf(lastId, maxId)
        val used = subjects.mapNotNull { it.image }.toSet()
        io.launch { Images.cleanup(dir, used) }
    }

    private fun readFile(): List<Subject> = try {
        if (file.exists()) deserialize(file.readText()) else emptyList()
    } catch (e: Exception) {
        runCatching { file.copyTo(File(dir, "subjects.broken.json"), overwrite = true) }
        emptyList()
    }

    private fun newId(): Long {
        lastId = maxOf(lastId + 1, System.currentTimeMillis())
        return lastId
    }

    fun subject(id: Long): Subject? = subjects.firstOrNull { it.id == id }

    // ---------- сортировка ----------

    /** Повторный выбор «по прогрессу» переворачивает направление. */
    fun selectSort(mode: SortMode) {
        if (mode == SortMode.PROGRESS && sortMode == SortMode.PROGRESS) {
            progressDescending = !progressDescending
        }
        sortMode = mode
        prefs.edit()
            .putInt("sort", mode.ordinal)
            .putBoolean("progress_desc", progressDescending)
            .apply()
    }

    // ---------- предметы ----------

    fun addSubject(name: String, teacher: String, image: String?, tab: Int): Long {
        val s = Subject(
            id = newId(),
            name = name,
            teacher = teacher,
            image = image,
            tab = tab,
            created = System.currentTimeMillis(),
            tasks = listOf(StudyTask(id = newId(), title = "Задание 1")),
        )
        commit(subjects + s)
        return s.id
    }

    fun updateSubject(id: Long, transform: (Subject) -> Subject) {
        commit(subjects.map { if (it.id == id) transform(it) else it })
    }

    fun moveSubject(id: Long) = updateSubject(id) {
        it.copy(tab = if (it.tab == TAB_CURRENT) TAB_DEBT else TAB_CURRENT)
    }

    fun deleteSubject(id: Long): Pair<Subject, Int>? {
        val i = subjects.indexOfFirst { it.id == id }
        if (i < 0) return null
        val removed = subjects[i]
        commit(subjects.toMutableList().apply { removeAt(i) })
        return removed to i
    }

    fun restoreSubject(subject: Subject, index: Int) {
        if (subjects.any { it.id == subject.id }) return
        commit(subjects.toMutableList().apply { add(index.coerceIn(0, size), subject) })
    }

    // ---------- задания ----------

    fun addTask(subjectId: Long): Long {
        val taskId = newId()
        updateSubject(subjectId) { s ->
            s.copy(tasks = s.tasks + StudyTask(id = taskId, title = "Задание ${s.tasks.size + 1}"))
        }
        return taskId
    }

    fun updateTask(subjectId: Long, taskId: Long, transform: (StudyTask) -> StudyTask) {
        updateSubject(subjectId) { s ->
            s.copy(tasks = s.tasks.map { if (it.id == taskId) transform(it) else it })
        }
    }

    fun deleteTask(subjectId: Long, taskId: Long): Pair<StudyTask, Int>? {
        val s = subject(subjectId) ?: return null
        val i = s.tasks.indexOfFirst { it.id == taskId }
        if (i < 0) return null
        val removed = s.tasks[i]
        updateSubject(subjectId) { it.copy(tasks = it.tasks.toMutableList().apply { removeAt(i) }) }
        return removed to i
    }

    fun restoreTask(subjectId: Long, task: StudyTask, index: Int) {
        updateSubject(subjectId) { s ->
            if (s.tasks.any { it.id == task.id }) s
            else s.copy(tasks = s.tasks.toMutableList().apply { add(index.coerceIn(0, size), task) })
        }
    }

    // ---------- сохранение ----------

    private fun commit(list: List<Subject>) {
        subjects = list
        val v = ++version
        val json = serialize(list)
        saveJob?.cancel()
        saveJob = io.launch {
            delay(300)
            write(json, v)
        }
    }

    /** Сохранить немедленно (вызывается, когда приложение уходит в фон). */
    fun flush() {
        saveJob?.cancel()
        write(serialize(subjects), version)
    }

    private fun write(json: String, v: Long) {
        synchronized(lock) {
            if (v <= writtenVersion) return
            val tmp = File(dir, "subjects.json.tmp")
            tmp.writeText(json)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
            writtenVersion = v
        }
    }

    companion object {
        @Volatile
        private var instance: Store? = null

        fun get(context: Context): Store =
            instance ?: synchronized(this) {
                instance ?: Store(context.applicationContext).also { instance = it }
            }
    }
}
