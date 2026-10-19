package com.kuzmin.studytracker.data

import org.json.JSONArray
import org.json.JSONObject

const val TAB_CURRENT = 0
const val TAB_DEBT = 1

/** Одно задание внутри предмета. deadline хранится как номер дня (LocalDate.toEpochDay). */
data class StudyTask(
    val id: Long,
    val title: String,
    val note: String = "",
    val deadline: Long? = null,
    val done: Boolean = false,
)

data class Subject(
    val id: Long,
    val name: String,
    val teacher: String,
    val image: String?,
    val tab: Int,
    val created: Long,
    val tasks: List<StudyTask>,
) {
    val doneCount: Int get() = tasks.count { it.done }

    /** Доля выполненных заданий: 2 из 10 = 0.2 */
    val progress: Float get() = if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size

    val isFinished: Boolean get() = tasks.isNotEmpty() && tasks.all { it.done }

    /** Ближайший срок среди невыполненных заданий */
    val nearestDeadline: Long?
        get() = tasks.asSequence().filter { !it.done }.mapNotNull { it.deadline }.minOrNull()
}

enum class SortMode { ADDED, PROGRESS, DEADLINE }

fun sortSubjects(list: List<Subject>, mode: SortMode, progressDescending: Boolean): List<Subject> =
    when (mode) {
        SortMode.ADDED -> list.sortedByDescending { it.created }
        SortMode.PROGRESS ->
            if (progressDescending) {
                list.sortedWith(compareByDescending<Subject> { it.progress }.thenByDescending { it.created })
            } else {
                list.sortedWith(compareBy<Subject> { it.progress }.thenByDescending { it.created })
            }
        SortMode.DEADLINE -> list.sortedWith(
            compareBy<Subject>(
                { it.isFinished },
                { it.nearestDeadline ?: Long.MAX_VALUE },
                { -it.created },
            )
        )
    }

internal fun serialize(list: List<Subject>): String {
    val arr = JSONArray()
    for (s in list) {
        val o = JSONObject()
        o.put("id", s.id)
        o.put("name", s.name)
        o.put("teacher", s.teacher)
        s.image?.let { o.put("image", it) }
        o.put("tab", s.tab)
        o.put("created", s.created)
        val tasks = JSONArray()
        for (t in s.tasks) {
            val to = JSONObject()
            to.put("id", t.id)
            to.put("title", t.title)
            to.put("note", t.note)
            t.deadline?.let { to.put("deadline", it) }
            to.put("done", t.done)
            tasks.put(to)
        }
        o.put("tasks", tasks)
        arr.put(o)
    }
    return JSONObject().put("version", 1).put("subjects", arr).toString()
}

internal fun deserialize(text: String): List<Subject> {
    val root = JSONObject(text)
    val arr = root.optJSONArray("subjects") ?: return emptyList()
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val ta = o.optJSONArray("tasks") ?: JSONArray()
        Subject(
            id = o.getLong("id"),
            name = o.optString("name"),
            teacher = o.optString("teacher"),
            image = if (o.has("image") && !o.isNull("image")) o.getString("image") else null,
            tab = o.optInt("tab", TAB_CURRENT),
            created = o.optLong("created", 0L),
            tasks = (0 until ta.length()).map { j ->
                val t = ta.getJSONObject(j)
                StudyTask(
                    id = t.getLong("id"),
                    title = t.optString("title"),
                    note = t.optString("note"),
                    deadline = if (t.has("deadline") && !t.isNull("deadline")) t.getLong("deadline") else null,
                    done = t.optBoolean("done"),
                )
            },
        )
    }
}
