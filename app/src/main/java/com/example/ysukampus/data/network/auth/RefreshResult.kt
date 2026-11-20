package com.example.ysukampus.data.network.auth

/**
 * Чем закончилась попытка обновить access-токен.
 *
 * Раньше [TokenRefresher] отвечал просто `String?`, и любое `null` означало
 * «сессия кончилась»: на любом исключении токены стирались. Из-за этого один
 * холодный старт без сети разлогинивал пользователя навсегда — валидный
 * refresh-токен удалялся из-за обычного `IOException`. Теперь отказ сервера и
 * недоступность сервера — разные вещи.
 */
sealed interface RefreshResult {
    /** Токен обновлён (или его уже успел обновить другой вызов). */
    data class Success(val accessToken: String) : RefreshResult

    /**
     * Сервер отказал: refresh-токена нет либо бэкенд ответил 4xx.
     * Токены очищены, `onNotAuthorized` вызван.
     */
    data object SessionExpired : RefreshResult

    /**
     * До сервера не достучались (нет сети, таймаут, 5xx, нечитаемый ответ).
     * Токены сохранены: повторить можно позже, разлогинивать не за что.
     */
    data class Unavailable(val error: Throwable?) : RefreshResult
}
