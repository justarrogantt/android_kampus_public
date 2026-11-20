package com.example.ysukampus.feature.registration.domain

import com.example.ysukampus.data.network.dto.UserDto

import com.example.ysukampus.data.auth.AuthStartResult
import com.example.ysukampus.data.session.AuthConfirmationResult

interface RegistrationInteractor {
    fun isDevAuthMode(): Boolean
    suspend fun restoreAuthorizedSession(): Boolean
    suspend fun beginAuthorization(): AuthStartResult
    suspend fun requestAuthentificationCode(): String
    suspend fun getAuthentificationStatus(code: String): String
    suspend fun confirmAuthentification(code: String): AuthConfirmationResult
    suspend fun getSessionsSummary(): String
    suspend fun logout(): String
    suspend fun logoutAll(): String
    /** Кэширует профиль после успешного входа — им приветствует главная. */
    fun saveCurrentUser(user: UserDto)
    fun onAuthorized()
    fun onNotAuthorized()
}
