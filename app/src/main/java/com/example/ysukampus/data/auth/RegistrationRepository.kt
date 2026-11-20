package com.example.ysukampus.data.auth

import com.example.ysukampus.data.session.AuthConfirmationResult
import com.example.ysukampus.data.network.dto.AuthentificationConfirmedResponse
import com.example.ysukampus.data.network.dto.AuthentificationStatusResponse
import com.example.ysukampus.data.network.dto.DevLoginRequestDto
import com.example.ysukampus.data.network.dto.NeedsRegistrationResponse
import com.example.ysukampus.data.session.TokenStorage
import okhttp3.ResponseBody
import kotlinx.serialization.json.Json
import retrofit2.Response

class RegistrationRepository(
    private val publicService: PublicService,
    private val authService: AuthService,
    private val tokenStorage: TokenStorage
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAuthentificationCode() = publicService.getAuthentificationCode().code

    suspend fun loginAsDev(request: DevLoginRequestDto): AuthentificationConfirmedResponse {
        val response = publicService.loginAsDev(request)
        tokenStorage.saveTokens(response.accessToken, response.refreshToken)
        return response
    }

    suspend fun getAuthentificationStatus(code: String): String {
        val response = publicService.getAuthentificationStatus(code)

        return when (response.code()) {
            200 -> json.decodeFromString<AuthentificationStatusResponse>(
                response.requireBodyText("GET auth/status/{code}")
            ).status
            else -> response.throwEndpointError("GET auth/status/{code}")
        }
    }

    suspend fun getAuthentificationConfirmation(code: String): AuthConfirmationResult {
        val response = authService.getAuthentificationConfirmation(code)

        return when (response.code()) {
            200 -> {
                val parsed = json.decodeFromString<AuthentificationConfirmedResponse>(
                    response.requireBodyText("POST auth/code/confirm/{code}")
                )
                tokenStorage.saveTokens(parsed.accessToken, parsed.refreshToken)
                AuthConfirmationResult.Success(parsed)
            }
            202 -> {
                json.decodeFromString<NeedsRegistrationResponse>(
                    response.requireBodyText("POST auth/code/confirm/{code}")
                )
                AuthConfirmationResult.NeedsRegistration
            }
            else -> response.throwEndpointError("POST auth/code/confirm/{code}")
        }
    }

    private fun Response<ResponseBody>.requireBodyText(endpoint: String): String {
        val bodyText = body()?.string().orEmpty()
        if (bodyText.isBlank()) {
            throw IllegalStateException("$endpoint вернул пустой ответ")
        }
        return bodyText
    }

    private fun Response<ResponseBody>.throwEndpointError(endpoint: String): Nothing {
        val details = errorBody()?.string()
            ?.takeIf { it.isNotBlank() }
            ?: message().takeIf { it.isNotBlank() }
            ?: "HTTP ${code()}"
        throw IllegalStateException("$endpoint вернул HTTP ${code()}: $details")
    }
}
