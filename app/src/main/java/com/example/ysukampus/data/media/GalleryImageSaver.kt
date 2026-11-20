package com.example.ysukampus.data.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Кладёт картинку в общий раздел «Картинки» галереи под альбомом ysukampus.
 *
 * Общая часть [com.example.ysukampus.data.forum.ForumImageDownloader] (картинки
 * форума, качаются анонимно) и скачивания оригинала фото профиля (требует
 * авторизованного запроса): обоим нужен один и тот же MediaStore-инсерт, а
 * различаются они только тем, откуда берутся байты.
 */
object GalleryImageSaver {
    suspend fun save(context: Context, input: InputStream, fileName: String, mimeType: String) {
        withContext(Dispatchers.IO) {
            val values = contentValues(fileName, mimeType)
            val resolver = context.contentResolver
            val uri = resolver.insert(imagesCollection(), values)
                ?: throw IOException("Галерея недоступна")

            try {
                resolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }
                    ?: throw IOException("Не удалось записать файл")
            } catch (error: Throwable) {
                // Иначе в галерее останется пустая запись без файла.
                resolver.delete(uri, null, null)
                throw error
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }
        }
    }

    private fun contentValues(fileName: String, mimeType: String) = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
            // Пока файл не дописан, галерея не должна его показывать.
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    private fun imagesCollection() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    private const val ALBUM = "YSU Kampus"
}
