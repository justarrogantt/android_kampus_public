package com.example.ysukampus.data.forum

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Сохранение картинки форума в галерею.
 *
 * Ручка `forum/media/{id}` открыта без авторизации — ссылка закрыта только
 * неугадываемым UUID, — поэтому качаем обычным клиентом без интерсепторов
 * авторизованного стека.
 */
class ForumImageDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    /**
     * Кладёт картинку по [imageUrl] в общий раздел «Картинки» галереи.
     *
     * Имя собираем из последнего сегмента ссылки — это уникальный id вложения,
     * так что сохранённые файлы не перетирают друг друга. Расширение спрашиваем
     * у сервера: в ссылке его нет, а на диске бэка лежит webp.
     */
    suspend fun downloadToGallery(imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(imageUrl).build()

            client.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    throw IOException("Сервер вернул ${response.code}")
                }

                val mimeType = body.contentType()
                    ?.takeIf { it.type == "image" }
                    ?.toString()
                    ?: DEFAULT_MIME_TYPE
                val values = contentValues(fileNameFor(imageUrl, mimeType), mimeType)

                val resolver = context.contentResolver
                val uri = resolver.insert(IMAGES_COLLECTION, values)
                    ?: throw IOException("Галерея недоступна")

                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        body.byteStream().use { input -> input.copyTo(output) }
                    } ?: throw IOException("Не удалось записать файл")
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
        }.recoverCatching { error -> throw ForumException(SAVE_ERROR_MESSAGE, error) }
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

    private fun fileNameFor(imageUrl: String, mimeType: String): String {
        val id = imageUrl.substringBefore('?').substringAfterLast('/').ifBlank { "image" }
        val extension = mimeType.substringAfter('/').substringBefore(';').ifBlank { "webp" }
        return "ysukampus-$id.$extension"
    }

    private companion object {
        val IMAGES_COLLECTION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        const val ALBUM = "YSU Kampus"
        const val DEFAULT_MIME_TYPE = "image/webp"
        const val SAVE_ERROR_MESSAGE = "Не удалось сохранить картинку"
    }
}
