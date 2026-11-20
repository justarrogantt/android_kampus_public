package com.example.ysukampus.data.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Подготовка выбранной пользователем картинки к multipart-загрузке.
 *
 * Общая часть фото профиля (`profile/photo`) и вложений форума (`forum/media`):
 * обе ручки принимают одно поле `file`, один набор форматов и один лимит
 * размера, а различаются только адресом и текстами ошибок.
 */

/** Лимит бэка (`MAX_PHOTO_SIZE_MB` в `app/config.py`). */
const val IMAGE_UPLOAD_MAX_SIZE_BYTES = 5L * 1024L * 1024L

/** Зеркало `ALLOWED_EXTENSIONS` из `app/utils/images.py`. */
val imageUploadSupportedExtensions = setOf("jpg", "jpeg", "png", "webp")
val imageUploadSupportedMimeTypes = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

/** Файл не прошёл локальную проверку. [message] уже готов к показу. */
class ImageUploadException(override val message: String) : Exception(message)

/**
 * Проверяет формат и размер картинки, копирует её во временный файл и отдаёт
 * готовую часть multipart-запроса в [block].
 *
 * Копия нужна потому, что `content://`-Uri не даёт длину заранее, а OkHttp
 * должен знать `Content-Length`. Временный файл удаляется в любом случае —
 * в том числе когда [block] бросил исключение.
 *
 * @param fileNamePrefix префикс временного файла и запасное имя, если провайдер
 * не отдал своё: бэк определяет расширение именно по имени.
 * @throws ImageUploadException формат или размер не подходят.
 */
suspend fun <T> withImageUploadPart(
    context: Context,
    uri: Uri,
    fileNamePrefix: String,
    tooLargeMessage: String,
    invalidFormatMessage: String,
    maxSizeBytes: Long = IMAGE_UPLOAD_MAX_SIZE_BYTES,
    block: suspend (MultipartBody.Part) -> T
): T {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)?.lowercase(Locale.ROOT)
    val originalName = resolveFileName(context, uri)
    val nameExtension = originalName
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase(Locale.ROOT)
        ?.takeIf(String::isNotBlank)
    val extension = resolveExtension(nameExtension, mimeType)

    if (mimeType != null && !mimeType.startsWith("image/")) {
        throw ImageUploadException(invalidFormatMessage)
    }
    val isMimeSupported = mimeType == null || mimeType in imageUploadSupportedMimeTypes
    val isExtensionSupported =
        extension in imageUploadSupportedExtensions && (nameExtension != null || mimeType != null)
    if (!isMimeSupported || !isExtensionSupported) {
        throw ImageUploadException(invalidFormatMessage)
    }

    val declaredSize = resolveFileSize(context, uri)
    if (declaredSize != null && declaredSize > maxSizeBytes) {
        throw ImageUploadException(tooLargeMessage)
    }

    val safeName = originalName ?: "$fileNamePrefix.$extension"
    val tempFile = withContext(Dispatchers.IO) {
        File.createTempFile("${fileNamePrefix}_", ".$extension", context.cacheDir)
    }

    return try {
        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Не удалось прочитать выбранный файл")
        }

        // Провайдер мог соврать про размер или не сообщить его вовсе.
        if (tempFile.length() > maxSizeBytes) {
            throw ImageUploadException(tooLargeMessage)
        }

        val mediaType = (mimeType ?: mimeForExtension(extension)).toMediaTypeOrNull()
        block(
            MultipartBody.Part.createFormData(
                name = "file",
                filename = safeName,
                body = tempFile.asRequestBody(mediaType)
            )
        )
    } finally {
        tempFile.delete()
    }
}

private fun resolveFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return it.getString(index)?.takeIf(String::isNotBlank)
            }
        }
    }
    return null
}

private fun resolveFileSize(context: Context, uri: Uri): Long? {
    val cursor = context.contentResolver
        .query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && !it.isNull(index)) {
                return it.getLong(index)
            }
        }
    }
    return null
}

private fun resolveExtension(nameExtension: String?, mimeType: String?): String {
    if (nameExtension in imageUploadSupportedExtensions) return nameExtension!!

    return when (mimeType) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> nameExtension ?: "jpg"
    }
}

private fun mimeForExtension(extension: String): String =
    when (extension.lowercase(Locale.ROOT)) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
