package com.example.ysukampus.data.forum

import android.content.Context
import com.example.ysukampus.data.media.GalleryImageSaver
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

                body.byteStream().use { input ->
                    GalleryImageSaver.save(
                        context = context,
                        input = input,
                        fileName = fileNameFor(imageUrl, mimeType),
                        mimeType = mimeType
                    )
                }
            }
        }.recoverCatching { error -> throw ForumException(SAVE_ERROR_MESSAGE, error) }
    }

    private fun fileNameFor(imageUrl: String, mimeType: String): String {
        val id = imageUrl.substringBefore('?').substringAfterLast('/').ifBlank { "image" }
        val extension = mimeType.substringAfter('/').substringBefore(';').ifBlank { "webp" }
        return "ysukampus-$id.$extension"
    }

    private companion object {
        const val DEFAULT_MIME_TYPE = "image/webp"
        const val SAVE_ERROR_MESSAGE = "Не удалось сохранить картинку"
    }
}
