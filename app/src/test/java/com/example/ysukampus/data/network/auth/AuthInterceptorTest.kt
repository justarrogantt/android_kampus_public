package com.example.ysukampus.data.network.auth

import com.example.ysukampus.data.auth.AuthService
import com.example.ysukampus.data.network.dto.RefreshRequestDTO
import com.example.ysukampus.data.network.dto.RefreshTokensResponse
import com.example.ysukampus.data.session.TokenStorage
import java.util.Base64
import java.util.concurrent.TimeUnit
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `AuthInterceptor` — единственное место, которое обновляет access-токен ДО
 * отправки запроса. Без этого опционально авторизованные ручки расписания
 * (см. `AccessTokenExpiry.kt`) на просроченный токен отвечают как
 * неавторизованному пользователю, а не 401, и `TokenAuthentificator` до них
 * не доходит.
 */
class AuthInterceptorTest {

    private class FakeTokenStorage(
        accessToken: String?,
        private val refreshToken: String? = "refresh"
    ) : TokenStorage {
        private var access: String? = accessToken
        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refreshToken
        override fun saveTokens(accessToken: String, refreshToken: String?) {
            access = accessToken
        }
        override fun clearTokens() { access = null }
    }

    private class FakeAuthService(
        private val respond: suspend (RefreshRequestDTO) -> RefreshTokensResponse
    ) : AuthService {
        override suspend fun getAuthentificationConfirmation(code: String) =
            throw NotImplementedError()

        override suspend fun refreshTokens(request: RefreshRequestDTO): RefreshTokensResponse =
            respond(request)
    }

    /** Записывает финальный запрос, ушедший в цепочку, и отвечает канонической пустышкой. */
    private class RecordingChain(private val original: Request) : Interceptor.Chain {
        var proceeded: Request? = null
            private set

        override fun request(): Request = original

        override fun proceed(request: Request): Response {
            proceeded = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        override fun connection(): Connection? = null
        override fun call() = throw NotImplementedError()
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
    }

    private fun jwtExpiringAt(nowSeconds: Long, deltaSeconds: Long): String {
        fun encode(json: String) =
            Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
        return "${encode("""{"alg":"none"}""")}.${encode("""{"exp":${nowSeconds + deltaSeconds}}""")}.sig"
    }

    private fun request(): Request = Request.Builder().url("https://example.test/schedule/day").build()

    @Test
    fun expiredTokenIsRefreshedBeforeRequestIsSent() {
        val now = System.currentTimeMillis() / 1000
        val expired = jwtExpiringAt(now, deltaSeconds = -60)
        val storage = FakeTokenStorage(accessToken = expired)
        val authService = FakeAuthService {
            RefreshTokensResponse(accessToken = "fresh-access", refreshToken = "fresh-refresh")
        }
        val interceptor = AuthInterceptor(storage, TokenRefresher(storage, authService))
        val chain = RecordingChain(request())

        interceptor.intercept(chain)

        assertEquals("Bearer fresh-access", chain.proceeded?.header("Authorization"))
    }

    @Test
    fun liveTokenIsSentAsIsWithoutRefreshCall() {
        val now = System.currentTimeMillis() / 1000
        val live = jwtExpiringAt(now, deltaSeconds = 3_600)
        val storage = FakeTokenStorage(accessToken = live)
        val authService = FakeAuthService { error("живой токен не должен вызывать refresh") }
        val interceptor = AuthInterceptor(storage, TokenRefresher(storage, authService))
        val chain = RecordingChain(request())

        interceptor.intercept(chain)

        assertEquals("Bearer $live", chain.proceeded?.header("Authorization"))
    }

    @Test
    fun noStoredTokenSendsRequestWithoutAuthorizationHeader() {
        val storage = FakeTokenStorage(accessToken = null)
        val authService = FakeAuthService { error("не должно вызываться") }
        val interceptor = AuthInterceptor(storage, TokenRefresher(storage, authService))
        val chain = RecordingChain(request())

        interceptor.intercept(chain)

        assertNull(chain.proceeded?.header("Authorization"))
    }

    /** Без tokenRefresher интерсептор ведёт себя как раньше: просто подставляет то, что есть. */
    @Test
    fun withoutRefresherExpiredTokenIsStillSentAsIs() {
        val now = System.currentTimeMillis() / 1000
        val expired = jwtExpiringAt(now, deltaSeconds = -60)
        val storage = FakeTokenStorage(accessToken = expired)
        val interceptor = AuthInterceptor(storage, tokenRefresher = null)
        val chain = RecordingChain(request())

        interceptor.intercept(chain)

        assertEquals("Bearer $expired", chain.proceeded?.header("Authorization"))
    }

    /** Refresh не смог обновить токен (сессия кончилась) — уходит старый, дальше сработает штатный 401. */
    @Test
    fun expiredTokenFallsBackToStaleTokenWhenRefreshFails() {
        val now = System.currentTimeMillis() / 1000
        val expired = jwtExpiringAt(now, deltaSeconds = -60)
        // Без refresh-токена TokenRefresher завершит сессию, не сходив в сеть.
        val storage = FakeTokenStorage(accessToken = expired, refreshToken = null)
        val authService = FakeAuthService { error("refresh-токена нет, до сети не дойдёт") }
        val interceptor = AuthInterceptor(storage, TokenRefresher(storage, authService))
        val chain = RecordingChain(request())

        interceptor.intercept(chain)

        assertEquals("Bearer $expired", chain.proceeded?.header("Authorization"))
    }
}
