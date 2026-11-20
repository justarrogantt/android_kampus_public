package com.example.ysukampus.data.auth

import com.example.ysukampus.data.network.dto.AuthentificationConfirmedResponse

sealed interface AuthStartResult {
    data class DevAuthorized(
        val response: AuthentificationConfirmedResponse
    ) : AuthStartResult

    data class TelegramCode(
        val code: String
    ) : AuthStartResult
}

class AuthRepository(
    private val authConfig: AuthConfig,
    private val registrationRepository: RegistrationRepository
) {
    val isDevMode: Boolean
        get() = authConfig.isDevMode

    suspend fun beginAuthorization(): AuthStartResult {
        return if (isDevMode) {
            AuthStartResult.DevAuthorized(
                registrationRepository.loginAsDev(authConfig.devLoginRequest)
            )
        } else {
            AuthStartResult.TelegramCode(
                registrationRepository.getAuthentificationCode()
            )
        }
    }
}
