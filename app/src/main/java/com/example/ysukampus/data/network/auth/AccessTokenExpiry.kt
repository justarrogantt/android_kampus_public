package com.example.ysukampus.data.network.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

/**
 * Проверка срока жизни access-токена по его собственному `exp`.
 *
 * Зачем это нужно, если есть [TokenAuthentificator]: тот просыпается только на
 * 401, а ручки с опциональной авторизацией (`schedule/day`, `schedule/week`)
 * на просроченный токен 401 не отдают — они просто перестают видеть
 * пользователя и отвечают 400 «Укажите параметр group или войдите, чтобы
 * открыть своё расписание». Refresh в таком случае не запускался вообще, и
 * расписание грузилось только если токен успел обновить кто-то другой — форум
 * или профиль, получившие свой честный 401. На первом запуске приложения
 * спасать было некому, и вкладка показывала «Не удалось загрузить расписание».
 *
 * Поэтому срок проверяется локально, до отправки запроса (см. [AuthInterceptor]).
 */

/** Запас перед истечением: токен, живущий меньше, уже считаем мёртвым. */
private const val EXPIRY_SKEW_SECONDS = 30L

/**
 * Истёк ли токен (с запасом [EXPIRY_SKEW_SECONDS]).
 *
 * Если срок вычитать не удалось — возвращаем `false`: неизвестный формат не
 * повод гонять refresh на каждом запросе, в этом случае остаётся штатный
 * путь через 401 и [TokenAuthentificator].
 */
internal fun isAccessTokenExpired(
    token: String,
    nowSeconds: Long = System.currentTimeMillis() / 1000
): Boolean {
    val expiresAt = accessTokenExpirySeconds(token) ?: return false
    return nowSeconds + EXPIRY_SKEW_SECONDS >= expiresAt
}

@Serializable
private data class JwtPayload(val exp: Long? = null)

private val payloadJson = Json { ignoreUnknownKeys = true }

/**
 * `java.util.Base64`, а не `android.util.Base64`: обе доступны с minSdk 26,
 * но у первой есть настоящая реализация и в JVM-юнит-тестах, а вторая в них —
 * только Android-заглушка, которая при вызове падает.
 */
private val urlDecoder = Base64.getUrlDecoder()

/** `exp` из payload'а JWT в секундах эпохи, либо `null`, если его там нет. */
private fun accessTokenExpirySeconds(token: String): Long? {
    // Подпись не проверяем и не можем: секрет только у бэкенда. Нам нужен лишь
    // срок, а подделывать его самому себе смысла нет — сервер всё равно
    // проверит токен по-настоящему.
    val payload = token.split('.').takeIf { it.size == 3 }?.get(1) ?: return null

    return try {
        val json = urlDecoder.decode(payload.withBase64Padding())
        payloadJson.decodeFromString(JwtPayload.serializer(), String(json)).exp?.takeIf { it > 0 }
    } catch (error: Exception) {
        // Битый base64 или не-JSON payload: ведём себя как при отсутствии срока.
        null
    }
}

/** JWT-сегменты идут без паддинга, а `java.util.Base64` его ожидает. */
private fun String.withBase64Padding(): String {
    val remainder = length % 4
    return if (remainder == 0) this else this + "=".repeat(4 - remainder)
}
