package com.example.ysukampus.data.network.auth

import com.example.ysukampus.data.auth.AuthService
import com.example.ysukampus.data.network.dto.RefreshRequestDTO
import com.example.ysukampus.data.session.TokenStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Единственная точка обновления access-токена.
 *
 * Логика жила внутри [TokenAuthentificator] и была доступна только OkHttp-стеку.
 * WebSocket-клиенту тоже нужно обновить токен (бэкенд закрывает сокет с 1008,
 * когда токен протух), поэтому она вынесена сюда: авторизованные Retrofit-клиенты
 * и сокеты делят один инстанс и один мьютекс, и параллельные отказы не вызывают
 * два конкурирующих refresh'а.
 */
class TokenRefresher(
    private val tokenStorage: TokenStorage,
    private val authService: AuthService,
    private val onNotAuthorized: (() -> Unit)? = null
) {
    private val refreshMutex = Mutex()

    /**
     * Возвращает актуальный access-токен: либо уже обновлённый кем-то другим,
     * либо полученный новым refresh-запросом. `null` — сессия закончилась,
     * [onNotAuthorized] уже вызван.
     *
     * @param failedToken токен, с которым запрос (или соединение) получил отказ.
     */
    suspend fun refreshAccessToken(failedToken: String?): String? = refreshMutex.withLock {
        val currentAccessToken = tokenStorage.getAccessToken()
        if (!currentAccessToken.isNullOrBlank() && currentAccessToken != failedToken) {
            // Пока мы ждали мьютекс, токен обновил кто-то другой.
            AuthLog.d("refresh: токен уже обновлён другим вызовом — берём его")
            return@withLock currentAccessToken
        }

        val refreshToken = tokenStorage.getRefreshToken()
        AuthLog.d(
            "refresh: в хранилище access=${AuthLog.describe(currentAccessToken)}, " +
                "refresh=${AuthLog.describe(refreshToken)}"
        )

        if (refreshToken == null) {
            AuthLog.d("refresh: refresh-токена нет — запрос не отправляем, разлогиниваем")
            tokenStorage.clearTokens()
            onNotAuthorized?.invoke()
            return@withLock null
        }

        val newTokens = try {
            AuthLog.d("refresh: отправляем POST auth/refresh")
            authService.refreshTokens(RefreshRequestDTO(refreshToken))
        } catch (error: Exception) {
            AuthLog.d("refresh: запрос упал — ${error.javaClass.simpleName}: ${error.message}")
            tokenStorage.clearTokens()
            onNotAuthorized?.invoke()
            return@withLock null
        }

        AuthLog.d(
            "refresh: сервер вернул access=${AuthLog.describe(newTokens.accessToken)}, " +
                "refresh=${AuthLog.describe(newTokens.refreshToken)}"
        )

        tokenStorage.saveTokens(
            accessToken = newTokens.accessToken,
            refreshToken = newTokens.refreshToken ?: refreshToken
        )
        newTokens.accessToken
    }
}
