package com.kuzmin.studytracker.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Копия всех данных в одном zip-файле: subjects.json (предметы, вкладки, задания)
 * и папка images с обложками.
 */
object Backup {

    private const val JSON_NAME = "subjects.json"
    private const val IMAGES_DIR = "images/"

    fun fileName(): String =
        "StudyTracker_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".zip"

    private fun write(subjects: List<Subject>, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            val files = LinkedHashMap<String, File>()
            // в копии вместо полного пути к обложке хранится только имя файла
            val portable = subjects.map { s ->
                val f = s.image?.let { File(it) }
                if (f != null && f.exists()) {
                    files[f.name] = f
                    s.copy(image = f.name)
                } else {
                    s.copy(image = null)
                }
            }
            zip.putNextEntry(ZipEntry(JSON_NAME))
            zip.write(serialize(portable).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for ((name, file) in files) {
                zip.putNextEntry(ZipEntry(IMAGES_DIR + name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Кладёт копию в Загрузки/StudyTracker. Возвращает имя файла или null при ошибке. */
    @RequiresApi(29)
    fun saveToDownloads(context: Context, subjects: List<Subject>): String? {
        val resolver = context.contentResolver
        val name = fileName()
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StudyTracker")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = try {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            null
        } ?: return null
        return try {
            val stream = resolver.openOutputStream(uri) ?: throw IllegalStateException("no stream")
            write(subjects, stream)
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            name
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            null
        }
    }

    /** Запись копии в файл, выбранный пользователем (для старых версий Android). */
    fun writeTo(context: Context, uri: Uri, subjects: List<Subject>): Boolean = try {
        val stream = context.contentResolver.openOutputStream(uri) ?: throw IllegalStateException("no stream")
        write(subjects, stream)
        true
    } catch (e: Exception) {
        false
    }

    /**
     * Читает копию: обложки распаковывает во внутреннюю память приложения.
     * Возвращает список предметов или null, если файл не является копией.
     */
    fun read(context: Context, uri: Uri): List<Subject>? = try {
        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val restored = HashMap<String, String>()
        var json: String? = null
        var counter = 0

        val input = context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("no stream")
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val entryName = entry.name
                if (!entry.isDirectory) {
                    if (entryName == JSON_NAME) {
                        json = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (entryName.startsWith(IMAGES_DIR)) {
                        val original = entryName.substringAfterLast('/')
                        val target = File(imagesDir, "img_${stamp}_${counter++}.jpg")
                        FileOutputStream(target).use { zip.copyTo(it) }
                        restored[original] = target.absolutePath
                    }
                }
                zip.closeEntry()
            }
        }

        val text = json
        if (text == null) {
            null
        } else {
            deserialize(text).map { s -> s.copy(image = s.image?.let { restored[it] }) }
        }
    } catch (e: Exception) {
        null
    }
}
