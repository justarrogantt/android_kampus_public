package com.example.ysukampus.data.session

import com.example.ysukampus.data.network.auth.RefreshResult
import java.util.Base64
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `SessionManager` решает, пускать ли пользователя в авторизованную зону на
 * старте, и чистит персональные данные при выходе. Оба пути завязаны на
 * [RefreshResult]: разлогинивать можно только на [RefreshResult.SessionExpired],
 * недоступность сервера ([RefreshResult.Unavailable]) не должна выкидывать на
 * регистрацию — иначе холодный старт офлайн стирал бы рабочую сессию.
 */
class SessionManagerTest {

    private class FakeTokenStorage(
        accessToken: String?,
        refreshToken: String?
    ) : TokenStorage {
        private var access: String? = accessToken
        private var refresh: String? = refreshToken
        var clearCount = 0
            private set

        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh
        override fun saveTokens(accessToken: String, refreshToken: String?) {
            access = accessToken
            refresh = refreshToken
        }
        override fun clearTokens() {
            access = null
            refresh = null
            clearCount++
        }
    }

    private fun jwtExpiringAt(nowSeconds: Long, deltaSeconds: Long): String {
        fun encode(json: String) =
            Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
        return "${encode("""{"alg":"none"}""")}.${encode("""{"exp":${nowSeconds + deltaSeconds}}""")}.sig"
    }

    private val now = System.currentTimeMillis() / 1000

    @Test
    fun noStoredTokensIsNotAuthorized() = runBlocking {
        val manager = SessionManager(FakeTokenStorage(accessToken = null, refreshToken = null))

        manager.restoreSession()

        assertEquals(AuthState.NotAuthorized, manager.authState.value)
    }

    @Test
    fun liveAccessTokenIsAuthorizedWithoutCallingRefresh() = runBlocking {
        val live = jwtExpiringAt(now, deltaSeconds = 3_600)
        val storage = FakeTokenStorage(accessToken = live, refreshToken = "refresh")
        val manager = SessionManager(storage, refreshAccessToken = { error("не должно вызываться") })

        manager.restoreSession()

        assertEquals(AuthState.Authorized, manager.authState.value)
    }

    @Test
    fun expiredTokenRefreshedSuccessfullyIsAuthorized() = runBlocking {
        val expired = jwtExpiringAt(now, deltaSeconds = -60)
        val storage = FakeTokenStorage(accessToken = expired, refreshToken = "refresh")
        val manager = SessionManager(
            storage,
            refreshAccessToken = { RefreshResult.Success("new-access") }
        )

        manager.restoreSession()

        assertEquals(AuthState.Authorized, manager.authState.value)
    }

    @Test
    fun expiredTokenWithSessionExpiredRefreshIsNotAuthorized() = runBlocking {
        val expired = jwtExpiringAt(now, deltaSeconds = -60)
        val storage = FakeTokenStorage(accessToken = expired, refreshToken = "refresh")
        val manager = SessionManager(
            storage,
            refreshAccessToken = { RefreshResult.SessionExpired }
        )

        manager.restoreSession()

        assertEquals(AuthState.NotAuthorized, manager.authState.value)
    }

    /** Сервер недоступен на холодном старте офлайн — сессию всё равно не рвём. */
    @Test
    fun expiredTokenWithUnavailableRefreshStaysAuthorized() = runBlocking {
        val expired = jwtExpiringAt(now, deltaSeconds = -60)
        val storage = FakeTokenStorage(accessToken = expired, refreshToken = "refresh")
        val manager = SessionManager(
            storage,
            refreshAccessToken = { RefreshResult.Unavailable(null) }
        )

        manager.restoreSession()

        assertEquals(AuthState.Authorized, manager.authState.value)
    }

    /** Без переданного refreshAccessToken — старое поведение: пускаем внутрь, дальше сработает 401. */
    @Test
    fun expiredTokenWithoutRefreshCallbackStaysAuthorized() = runBlocking {
        val expired = jwtExpiringAt(now, deltaSeconds = -60)
        val storage = FakeTokenStorage(accessToken = expired, refreshToken = "refresh")
        val manager = SessionManager(storage, refreshAccessToken = null)

        manager.restoreSession()

        assertEquals(AuthState.Authorized, manager.authState.value)
    }

    @Test
    fun onNotAuthorizedClearsTokensAndNotifiesCacheCleanup() {
        val storage = FakeTokenStorage(accessToken = "access", refreshToken = "refresh")
        var cacheClearedCount = 0
        val manager = SessionManager(storage, onSessionCleared = { cacheClearedCount++ })

        manager.onNotAuthorized()

        assertEquals(1, storage.clearCount)
        assertEquals(1, cacheClearedCount)
        assertEquals(AuthState.NotAuthorized, manager.authState.value)
    }
}
