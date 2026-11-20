package com.example.ysukampus.data.network.auth

import com.example.ysukampus.data.auth.AuthService
import com.example.ysukampus.data.network.dto.RefreshRequestDTO
import com.example.ysukampus.data.session.TokenStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

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
     * либо полученный новым refresh-запросом. `null` — обновить не вышло,
     * причину см. в [refresh].
     *
     * @param failedToken токен, с которым запрос (или соединение) получил отказ.
     */
    suspend fun refreshAccessToken(failedToken: String?): String? =
        (refresh(failedToken) as? RefreshResult.Success)?.accessToken

    /**
     * То же, что [refreshAccessToken], но с причиной отказа.
     *
     * Токены стираются только при [RefreshResult.SessionExpired] — когда сервер
     * действительно сказал «нет». Недоступность сервера ([RefreshResult.Unavailable])
     * сессию не заканчивает: иначе запуск в оффлайне удалял бы рабочий
     * refresh-токен и требовал логиниться заново.
     */
    suspend fun refresh(failedToken: String?): RefreshResult = refreshMutex.withLock {
        val currentAccessToken = tokenStorage.getAccessToken()
        if (!currentAccessToken.isNullOrBlank() && currentAccessToken != failedToken) {
            // Пока мы ждали мьютекс, токен обновил кто-то другой.
            AuthLog.d("refresh: токен уже обновлён другим вызовом — берём его")
            return@withLock RefreshResult.Success(currentAccessToken)
        }

        val refreshToken = tokenStorage.getRefreshToken()
        AuthLog.d(
            "refresh: в хранилище access=${AuthLog.describe(currentAccessToken)}, " +
                "refresh=${AuthLog.describe(refreshToken)}"
        )

        if (refreshToken == null) {
            AuthLog.d("refresh: refresh-токена нет — запрос не отправляем, разлогиниваем")
            return@withLock sessionExpired()
        }

        val newTokens = try {
            AuthLog.d("refresh: отправляем POST auth/refresh")
            authService.refreshTokens(RefreshRequestDTO(refreshToken))
        } catch (error: HttpException) {
            AuthLog.d("refresh: сервер ответил ${error.code()}")
            // 4xx — refresh-токен сервер не принял, это конец сессии.
            // 5xx — авария на бэкенде, токены у нас по-прежнему рабочие.
            return@withLock if (error.code() in 400..499) {
                sessionExpired()
            } else {
                RefreshResult.Unavailable(error)
            }
        } catch (error: Exception) {
            // IOException (нет сети, таймаут), разбор ответа и прочее: сервер
            // ничего не отверг, значит и разлогинивать не за что.
            AuthLog.d("refresh: запрос упал — ${error.javaClass.simpleName}: ${error.message}")
            return@withLock RefreshResult.Unavailable(error)
        }

        AuthLog.d(
            "refresh: сервер вернул access=${AuthLog.describe(newTokens.accessToken)}, " +
                "refresh=${AuthLog.describe(newTokens.refreshToken)}"
        )

        tokenStorage.saveTokens(
            accessToken = newTokens.accessToken,
            refreshToken = newTokens.refreshToken ?: refreshToken
        )
        RefreshResult.Success(newTokens.accessToken)
    }

    private fun sessionExpired(): RefreshResult {
        tokenStorage.clearTokens()
        onNotAuthorized?.invoke()
        return RefreshResult.SessionExpired
    }
}
