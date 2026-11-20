package com.example.ysukampus.data.auth

import android.content.Context
import com.example.ysukampus.data.media.GalleryImageSaver
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Сохранение оригинала своего фото профиля в галерею — через
 * `profile/photo/download`, а не через публичный `profile/avatar/{id}`: там
 * лежит уменьшенный квадратный аватар, а здесь то, что было загружено.
 *
 * Ручка отдаёт только владельцу, поэтому качаем авторизованным клиентом
 * ([AuthorizedService]), а не голым OkHttp, как форумные картинки
 * ([com.example.ysukampus.data.forum.ForumImageDownloader]) — им авторизация
 * не нужна вовсе. Сама запись в галерею — общий путь, [GalleryImageSaver].
 */
class ProfilePhotoDownloader(
    private val context: Context,
    private val authorizedService: AuthorizedService
) {
    suspend fun downloadToGallery(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = authorizedService.downloadProfilePhoto()
            val mimeType = body.contentType()
                ?.takeIf { it.type == "image" }
                ?.toString()
                ?: DEFAULT_MIME_TYPE
            val extension = mimeType.substringAfter('/').substringBefore(';').ifBlank { "jpg" }

            body.byteStream().use { input ->
                GalleryImageSaver.save(
                    context = context,
                    input = input,
                    fileName = "ysukampus-photo-${System.currentTimeMillis()}.$extension",
                    mimeType = mimeType
                )
            }
        }.recoverCatching { error -> throw error.toDownloadException() }
    }

    private fun Throwable.toDownloadException(): Exception = when (this) {
        is IOException -> IllegalStateException(NETWORK_ERROR_MESSAGE, this)
        is HttpException -> IllegalStateException(SAVE_ERROR_MESSAGE, this)
        else -> IllegalStateException(SAVE_ERROR_MESSAGE, this)
    }

    private companion object {
        const val DEFAULT_MIME_TYPE = "image/jpeg"
        const val NETWORK_ERROR_MESSAGE =
            "Не удалось связаться с сервером. Проверьте соединение с интернетом"
        const val SAVE_ERROR_MESSAGE = "Не удалось сохранить фото"
    }
}
