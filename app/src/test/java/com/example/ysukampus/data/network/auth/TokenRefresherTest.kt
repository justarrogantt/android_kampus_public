package com.example.ysukampus.data.network.auth

import com.example.ysukampus.data.auth.AuthService
import com.example.ysukampus.data.network.dto.RefreshRequestDTO
import com.example.ysukampus.data.network.dto.RefreshTokensResponse
import com.example.ysukampus.data.session.TokenStorage
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * `TokenRefresher.refresh` — единственная точка обновления access-токена.
 * Обе регрессии, которые уже случались в проде, разбиты по сути на одно
 * правило: разлогинивать можно только когда СЕРВЕР сказал «нет» (4xx на
 * auth/refresh, отсутствующий refresh-токен), а не когда до него просто не
 * достучались (`IOException`, таймаут, 5xx, нечитаемый ответ) — тогда токены
 * должны остаться на месте.
 */
class TokenRefresherTest {

    private class FakeTokenStorage(
        accessToken: String? = null,
        refreshToken: String? = null
    ) : TokenStorage {
        private var access: String? = accessToken
        private var refresh: String? = refreshToken
        var clearCount = 0
            private set
        var saveCount = 0
            private set
        var lastSavedAccess: String? = null
            private set
        var lastSavedRefresh: String? = null
            private set

        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh

        override fun saveTokens(accessToken: String, refreshToken: String?) {
            access = accessToken
            refresh = refreshToken
            lastSavedAccess = accessToken
            lastSavedRefresh = refreshToken
            saveCount++
        }

        override fun clearTokens() {
            access = null
            refresh = null
            clearCount++
        }
    }

    private class FakeAuthService(
        private val respond: suspend (RefreshRequestDTO) -> RefreshTokensResponse
    ) : AuthService {
        var callCount = 0
            private set

        override suspend fun getAuthentificationConfirmation(code: String) =
            throw NotImplementedError("не нужен в этих тестах")

        override suspend fun refreshTokens(request: RefreshRequestDTO): RefreshTokensResponse {
            callCount++
            return respond(request)
        }
    }

    private fun httpException(code: Int): HttpException {
        val body: ResponseBody = "".toResponseBody(null)
        val response = Response.error<RefreshTokensResponse>(code, body)
        return HttpException(response)
    }

    private fun refresher(
        tokenStorage: FakeTokenStorage,
        authService: FakeAuthService,
        onNotAuthorized: () -> Unit = {}
    ) = TokenRefresher(tokenStorage, authService, onNotAuthorized)

    // --- SessionExpired: сервер отказал ---

    @Test
    fun missingRefreshTokenClearsTokensWithoutCallingServer() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = null)
        val authService = FakeAuthService { error("не должно вызываться") }
        var notifiedCount = 0

        val result = refresher(storage, authService, onNotAuthorized = { notifiedCount++ })
            .refresh(failedToken = "old-access")

        assertEquals(RefreshResult.SessionExpired, result)
        assertEquals(0, authService.callCount)
        assertEquals(1, storage.clearCount)
        assertEquals(1, notifiedCount)
    }

    @Test
    fun serverRejects4xxClearsTokensAndEndsSession() = runBlocking {
        for (code in listOf(400, 401, 403, 404, 422)) {
            val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
            val authService = FakeAuthService { throw httpException(code) }
            var notifiedCount = 0

            val result = refresher(storage, authService, onNotAuthorized = { notifiedCount++ })
                .refresh(failedToken = "old-access")

            assertEquals("код $code должен завершать сессию", RefreshResult.SessionExpired, result)
            assertEquals("код $code должен чистить токены", 1, storage.clearCount)
            assertEquals("код $code должен звать onNotAuthorized", 1, notifiedCount)
        }
    }

    // --- Unavailable: сервер не виноват, токены целы ---

    @Test
    fun serverError5xxKeepsTokensAndReportsUnavailable() = runBlocking {
        for (code in listOf(500, 502, 503)) {
            val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
            val authService = FakeAuthService { throw httpException(code) }
            var notifiedCount = 0

            val result = refresher(storage, authService, onNotAuthorized = { notifiedCount++ })
                .refresh(failedToken = "old-access")

            assertTrue("код $code должен быть Unavailable", result is RefreshResult.Unavailable)
            assertEquals("код $code не должен чистить токены", 0, storage.clearCount)
            assertEquals("код $code не должен звать onNotAuthorized", 0, notifiedCount)
        }
    }

    @Test
    fun ioExceptionKeepsTokensAndReportsUnavailable() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
        val error = IOException("нет сети")
        val authService = FakeAuthService { throw error }

        val result = refresher(storage, authService).refresh(failedToken = "old-access")

        assertEquals(RefreshResult.Unavailable(error), result)
        assertEquals(0, storage.clearCount)
        assertEquals("old-access", storage.getAccessToken())
        assertEquals("old-refresh", storage.getRefreshToken())
    }

    @Test
    fun timeoutKeepsTokensAndReportsUnavailable() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
        val error = SocketTimeoutException("timeout")
        val authService = FakeAuthService { throw error }

        val result = refresher(storage, authService).refresh(failedToken = "old-access")

        assertTrue(result is RefreshResult.Unavailable)
        assertEquals(0, storage.clearCount)
    }

    /** Нечитаемый/непарсящийся ответ — тоже не повод разлогинивать. */
    @Test
    fun unparseableResponseKeepsTokensAndReportsUnavailable() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
        val error = IllegalStateException("не смогли разобрать тело ответа")
        val authService = FakeAuthService { throw error }

        val result = refresher(storage, authService).refresh(failedToken = "old-access")

        assertEquals(RefreshResult.Unavailable(error), result)
        assertEquals(0, storage.clearCount)
    }

    // --- Success ---

    @Test
    fun successSavesRotatedTokensAndReturnsNewAccessToken() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
        val authService = FakeAuthService {
            assertEquals("old-refresh", it.refreshToken)
            RefreshTokensResponse(accessToken = "new-access", refreshToken = "new-refresh")
        }

        val result = refresher(storage, authService).refresh(failedToken = "old-access")

        assertEquals(RefreshResult.Success("new-access"), result)
        assertEquals(1, storage.saveCount)
        assertEquals("new-access", storage.lastSavedAccess)
        assertEquals("new-refresh", storage.lastSavedRefresh)
    }

    /** Бэкенд не обязан ротировать refresh-токен на каждый вызов. */
    @Test
    fun successWithoutRotatedRefreshTokenKeepsOldOne() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
        val authService = FakeAuthService {
            RefreshTokensResponse(accessToken = "new-access", refreshToken = null)
        }

        refresher(storage, authService).refresh(failedToken = "old-access")

        assertEquals("old-refresh", storage.lastSavedRefresh)
    }

    /** Пока мы ждали мьютекс, токен уже обновил кто-то другой — в сеть не идём. */
    @Test
    fun tokenAlreadyRefreshedByAnotherCallIsReusedWithoutNetworkCall() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "already-new", refreshToken = "old-refresh")
        val authService = FakeAuthService { error("не должно вызываться") }

        val result = refresher(storage, authService).refresh(failedToken = "old-access")

        assertEquals(RefreshResult.Success("already-new"), result)
        assertEquals(0, authService.callCount)
    }

    // --- refreshAccessToken: старый интерфейс поверх refresh() ---

    @Test
    fun refreshAccessTokenHelperReturnsTokenOnSuccess() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
        val authService = FakeAuthService {
            RefreshTokensResponse(accessToken = "new-access", refreshToken = "new-refresh")
        }

        val token = refresher(storage, authService).refreshAccessToken(failedToken = "old-access")

        assertEquals("new-access", token)
    }

    @Test
    fun refreshAccessTokenHelperReturnsNullOnSessionExpired() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = null)
        val authService = FakeAuthService { error("не должно вызываться") }

        val token = refresher(storage, authService).refreshAccessToken(failedToken = "old-access")

        assertNull(token)
    }

    @Test
    fun refreshAccessTokenHelperReturnsNullOnUnavailable() = runBlocking {
        val storage = FakeTokenStorage(accessToken = "old-access", refreshToken = "old-refresh")
        val authService = FakeAuthService { throw IOException("нет сети") }

        val token = refresher(storage, authService).refreshAccessToken(failedToken = "old-access")

        assertNull(token)
        assertFalse("Unavailable не должен чистить токены", storage.clearCount > 0)
    }
}
