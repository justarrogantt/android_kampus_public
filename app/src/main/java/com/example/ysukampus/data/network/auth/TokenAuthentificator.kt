package com.example.ysukampus.data.network.auth

import kotlinx.coroutines.runBlocking
import com.example.ysukampus.data.session.TokenStorage
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthentificator(
    private val tokenStorage: TokenStorage,
    private val tokenRefresher: TokenRefresher,
    private val onNotAuthorized: (() -> Unit)? = null
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        AuthLog.d(
            "authenticate: ${response.code} ${response.request.url.encodedPath}, " +
                "попытка ${responseCount(response)}"
        )

        if (responseCount(response) >= 2) {
            AuthLog.d("authenticate: вторая неудача подряд — чистим токены и разлогиниваем")
            tokenStorage.clearTokens()
            onNotAuthorized?.invoke()
            return null
        }

        val failedToken = response.request.header(AUTHORIZATION_HEADER)
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()

        AuthLog.d("authenticate: отвергнутый access-токен ${AuthLog.describe(failedToken)}")

        val refreshResult = runBlocking {
            tokenRefresher.refresh(failedToken)
        }

        if (refreshResult is RefreshResult.Unavailable) {
            // Сервер до refresh'а не дожил (сеть отвалилась между 401 и
            // обновлением, 5xx). Токены целы — разлогинивать не за что,
            // запрос просто останется неуспешным.
            AuthLog.d("authenticate: refresh недоступен — сессию сохраняем, запрос останется с 401")
            return null
        }

        val updatedAccessToken = (refreshResult as? RefreshResult.Success)?.accessToken

        if (updatedAccessToken.isNullOrBlank()) {
            AuthLog.d("authenticate: обновить не удалось — запрос останется с 401")
            onNotAuthorized?.invoke()
            return null
        }

        AuthLog.d("authenticate: токен обновлён, повторяем запрос")

        return response.request.newBuilder()
            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$updatedAccessToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
