package com.kuzmin.studytracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/** Фото предметов: выбранная картинка обрезается в квадрат по центру и сохраняется к себе. */
object Images {

    private const val SIZE = 640
    private val cache = LruCache<String, ImageBitmap>(32)

    private fun imagesDir(base: File) = File(base, "images").apply { mkdirs() }

    fun load(path: String): ImageBitmap? {
        cache.get(path)?.let { return it }
        val bmp = BitmapFactory.decodeFile(path) ?: return null
        return bmp.asImageBitmap().also { cache.put(path, it) }
    }

    /** Вызывать в фоновом потоке. Возвращает путь к сохранённому квадратному JPEG. */
    fun importSquare(context: Context, uri: Uri): String? = try {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            val minSide = min(bounds.outWidth, bounds.outHeight)
            while (minSide / (sample * 2) >= SIZE) sample *= 2

            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
            if (decoded == null) {
                null
            } else {
                val orientation = try {
                    resolver.openInputStream(uri)?.use {
                        ExifInterface(it).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
                } catch (e: Exception) {
                    ExifInterface.ORIENTATION_NORMAL
                }

                val side = min(decoded.width, decoded.height)
                val x = (decoded.width - side) / 2
                val y = (decoded.height - side) / 2
                val scale = min(1f, SIZE.toFloat() / side)

                val m = Matrix()
                m.setScale(scale, scale)
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
                    ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
                }

                val square = Bitmap.createBitmap(decoded, x, y, side, side, m, true)
                val out = File(imagesDir(context.filesDir), "img_${System.currentTimeMillis()}.jpg")
                FileOutputStream(out).use { square.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                out.absolutePath
            }
        }
    } catch (e: Exception) {
        null
    }

    /** Удаляет картинки, которые больше не привязаны ни к одному предмету. */
    fun cleanup(base: File, used: Set<String>) {
        imagesDir(base).listFiles()?.forEach { f ->
            if (f.absolutePath !in used) f.delete()
        }
    }
}
