package com.example.ysukampus.data.auth

import com.example.ysukampus.data.session.DeviceIdStorage
import com.example.ysukampus.data.network.dto.GetTokensResponse
import com.example.ysukampus.data.network.dto.TokensDTO
import com.example.ysukampus.data.session.SessionManager
import com.example.ysukampus.data.session.TokenStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class SessionRepository(
    private val authorizedService: AuthorizedService,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val deviceIdStorage: DeviceIdStorage
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getTokens(): GetTokensResponse = authorizedService.getTokens()
    fun getCurrentDeviceId(): String = deviceIdStorage.getOrCreateDeviceId()

    fun isCurrentSession(session: TokensDTO): Boolean {
        val currentDeviceId = getCurrentDeviceId()
        return session.id == currentDeviceId ||
            session.deviceInfo.equals(currentDeviceId, ignoreCase = true) ||
            session.deviceInfo.contains(currentDeviceId, ignoreCase = true)
    }

    suspend fun logout(): String {
        val response = authorizedService.logout()
        val successBody = response.body()?.string().orEmpty()
        if (!response.isSuccessful) {
            val errorMessage = extractMessage(response.errorBody()?.string().orEmpty())
                ?: "HTTP ${response.code()}"
            throw IllegalStateException(errorMessage)
        }
        tokenStorage.clearTokens()
        sessionManager.onNotAuthorized()
        return extractMessage(successBody) ?: "Успешно"
    }

    suspend fun logoutAll(): String {
        val response = authorizedService.logoutAll()
        val successBody = response.body()?.string().orEmpty()
        if (!response.isSuccessful) {
            val errorMessage = extractMessage(response.errorBody()?.string().orEmpty())
                ?: "HTTP ${response.code()}"
            throw IllegalStateException(errorMessage)
        }
        tokenStorage.clearTokens()
        sessionManager.onNotAuthorized()
        return extractMessage(successBody) ?: "Успешно"
    }

    private fun extractMessage(rawBody: String): String? {
        if (rawBody.isBlank()) return null

        return runCatching {
            val obj = json.decodeFromString<JsonObject>(rawBody)
            obj["message"]?.jsonPrimitive?.content
                ?: obj["response"]?.jsonPrimitive?.content
                ?: obj["detail"]?.jsonPrimitive?.content
        }.getOrNull() ?: rawBody
    }
}
