package com.example.ysukampus.data.network.auth

import com.example.ysukampus.data.session.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Подставляет access-токен и обновляет его заранее, если срок уже вышел.
 *
 * Проактивный refresh появился из-за ручек с опциональной авторизацией: они на
 * просроченный токен отвечают не 401, а «как неавторизованному», поэтому
 * [TokenAuthentificator] до них не доходит (подробности — в [isAccessTokenExpired]).
 * Обновление идёт через тот же процессный [TokenRefresher], что и путь 401, —
 * общий мьютекс не даёт двум отказам породить два refresh-запроса.
 *
 * `tokenRefresher` необязателен: без него интерсептор ведёт себя как прежде и
 * только подставляет заголовок.
 */
class AuthInterceptor(
    private val tokenStorage: TokenStorage,
    private val tokenRefresher: TokenRefresher? = null
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { currentAccessToken() }
        val request = chain.request().newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }

    private suspend fun currentAccessToken(): String? {
        val stored = tokenStorage.getAccessToken() ?: return null
        if (tokenRefresher == null || !isAccessTokenExpired(stored)) return stored

        AuthLog.d("auth: access-токен истёк — обновляем до отправки запроса")

        // null — сессия закончилась, TokenRefresher уже сообщил об этом
        // SessionManager'у. Отправляем запрос со старым токеном: 401 и
        // разлогин по нему — уже отлаженный путь.
        return tokenRefresher.refreshAccessToken(stored) ?: stored
    }
}
