package com.example.ysukampus.data.auth

import android.content.Context
import android.net.Uri
import com.example.ysukampus.data.media.ImageUploadException
import com.example.ysukampus.data.media.withImageUploadPart
import com.example.ysukampus.data.network.dto.ProfilePhotoDto
import com.example.ysukampus.data.network.dto.TelegramNotificationsRequestDto
import com.example.ysukampus.data.network.dto.UserProfileDto
import com.example.ysukampus.data.network.dto.UserUpdateRequestDto
import com.example.ysukampus.data.network.error.toReadableMessage
import retrofit2.HttpException

class ProfileRepository(
    private val context: Context,
    private val authorizedService: AuthorizedService
) {
    /**
     * Загружает фото профиля.
     *
     * Чтение Uri, проверка формата/размера и сборка multipart вынесены в
     * [withImageUploadPart] — тем же путём уходит вложение форума.
     */
    suspend fun uploadProfilePhoto(userId: Long, uri: Uri): Result<ProfilePhotoDto> = runCatching {
        withImageUploadPart(
            context = context,
            uri = uri,
            fileNamePrefix = "profile_photo",
            tooLargeMessage = FILE_TOO_LARGE_MESSAGE,
            invalidFormatMessage = INVALID_FORMAT_MESSAGE
        ) { part -> authorizedService.uploadUserAvatar(userId, part) }
    }.recoverCatching { throwable -> throw mapUploadError(throwable) }

    suspend fun getProfilePhoto(): Result<ProfilePhotoDto?> {
        return runCatching { authorizedService.getProfilePhoto() }
            .recoverCatching { throwable ->
                val httpException = throwable as? HttpException
                when (httpException?.code()) {
                    404 -> null
                    else -> throw IllegalStateException(throwable.toReadableMessage(), throwable)
                }
            }
    }

    suspend fun deleteProfilePhoto(userId: Long): Result<Unit> {
        return runCatching {
            val response = authorizedService.deleteUserAvatar(userId)
            when {
                response.isSuccessful || response.code() == 404 -> Unit
                else -> throw HttpException(response)
            }
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.toReadableMessage(), throwable)
        }
    }

    /** Свежая карточка пользователя: ник, цвет, группа могли поменяться с другого устройства. */
    suspend fun getUser(userId: Long): Result<UserProfileDto> {
        return runCatching { authorizedService.getUser(userId) }
            .recoverCatching { throwable ->
                throw IllegalStateException(throwable.toReadableMessage(), throwable)
            }
    }

    /** Сохраняет отредактированный профиль. Вызывающий шлёт только изменённые поля. */
    suspend fun updateUser(userId: Long, body: UserUpdateRequestDto): Result<UserProfileDto> {
        return runCatching { authorizedService.updateUser(userId, body) }
            .recoverCatching { throwable -> throw mapUpdateError(throwable) }
    }

    /**
     * Тумблер дублирования уведомлений в Telegram.
     *
     * Своя ручка, а не поле в `PUT ref/user/{id}`: тумблер часто меняется в
     * одиночку, а профиль без остальных изменений бэк отбил бы четырёхсоткой
     * «не переданы поля для обновления».
     */
    suspend fun updateTelegramNotifications(enabled: Boolean): Result<Boolean> {
        return runCatching {
            authorizedService.updateTelegramNotifications(
                TelegramNotificationsRequestDto(enabled = enabled)
            ).enabled
        }.recoverCatching { throwable ->
            throw IllegalStateException(
                throwable.toReadableMessage(),
                throwable
            )
        }
    }

    /**
     * Случайный анекдот, не повторяющий уже показанные в этом сеансе шторки.
     * Когда бэк исчерпал набор, он сам начинает новый круг.
     */
    suspend fun getJoke(seenIds: List<Int>): Result<ProfileJoke> {
        return runCatching {
            val seen = seenIds.takeIf { it.isNotEmpty() }?.joinToString(",")
            val dto = authorizedService.getProfileJoke(seen)
            ProfileJoke(id = dto.id, text = dto.text)
        }.recoverCatching { throwable ->
            throw IllegalStateException(throwable.toReadableMessage(), throwable)
        }
    }

    private fun mapUploadError(throwable: Throwable): Throwable {
        // Локальную проверку файла показываем как есть: текст уже готов.
        if (throwable is ImageUploadException) return throwable

        val httpException = throwable as? HttpException
        val message = when (httpException?.code()) {
            413 -> FILE_TOO_LARGE_MESSAGE
            422 -> INVALID_FORMAT_MESSAGE
            423 -> "Смена аватара временно запрещена."
            else -> throwable.toReadableMessage()
        }
        return IllegalStateException(message, throwable)
    }

    /**
     * Ответы `PUT ref/user/{id}` различаются только кодом, поэтому понятный
     * текст собираем здесь, а не разбираем тело в ViewModel.
     */
    private fun mapUpdateError(throwable: Throwable): Throwable {
        val httpException = throwable as? HttpException
        val message = when (httpException?.code()) {
            400 -> "Не переданы поля для обновления."
            403 -> "Это поле пока нельзя изменить."
            409 -> "Такой ник уже занят."
            422 -> "Проверьте введённые данные."
            423 -> "Смена ника временно запрещена."
            429 -> "Менять ник можно раз в 30 дней."
            else -> throwable.toReadableMessage()
        }
        return IllegalStateException(message, throwable)
    }

    private companion object {
        const val FILE_TOO_LARGE_MESSAGE = "Фото слишком большое. Максимальный размер - 5 MB."
        const val INVALID_FORMAT_MESSAGE = "Неверный формат изображения. Поддерживаются JPG, PNG и WEBP."
    }
}

/** Анекдот для профиля: id нужен только чтобы не повторяться при запросе следующего. */
data class ProfileJoke(val id: Int, val text: String)
