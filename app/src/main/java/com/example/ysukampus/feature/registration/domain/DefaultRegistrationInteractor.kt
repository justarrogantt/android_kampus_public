package com.example.ysukampus.feature.registration.domain

import com.example.ysukampus.data.auth.AuthRepository
import com.example.ysukampus.data.auth.AuthStartResult
import com.example.ysukampus.data.auth.RegistrationRepository
import com.example.ysukampus.data.auth.SessionRepository
import com.example.ysukampus.data.network.dto.UserDto
import com.example.ysukampus.data.session.AuthConfirmationResult
import com.example.ysukampus.data.session.CurrentUserStore
import com.example.ysukampus.data.session.AuthState
import com.example.ysukampus.data.session.SessionManager

class DefaultRegistrationInteractor(
    private val authRepository: AuthRepository,
    private val registrationRepository: RegistrationRepository,
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager,
    private val currentUserStore: CurrentUserStore,
    private val sessionsSummaryFormatter: SessionsSummaryFormatter
) : RegistrationInteractor {

    override fun isDevAuthMode(): Boolean = authRepository.isDevMode

    override suspend fun restoreAuthorizedSession(): Boolean {
        sessionManager.restoreSession()
        return sessionManager.authState.value == AuthState.Authorized
    }

    override suspend fun beginAuthorization(): AuthStartResult {
        return authRepository.beginAuthorization()
    }

    override suspend fun requestAuthentificationCode(): String {
        return registrationRepository.getAuthentificationCode()
    }

    override suspend fun getAuthentificationStatus(code: String): String {
        return registrationRepository.getAuthentificationStatus(code)
    }

    override suspend fun confirmAuthentification(code: String): AuthConfirmationResult {
        return registrationRepository.getAuthentificationConfirmation(code)
    }

    override suspend fun getSessionsSummary(): String {
        val sessions = sessionRepository.getTokens().tokens
        return sessionsSummaryFormatter.format(sessions, sessionRepository::isCurrentSession)
    }

    override suspend fun logout(): String {
        return sessionRepository.logout()
    }

    override suspend fun logoutAll(): String {
        return sessionRepository.logoutAll()
    }

    override fun saveCurrentUser(user: UserDto) {
        currentUserStore.save(user)
    }

    override fun onAuthorized() {
        sessionManager.onAuthorized()
    }

    override fun onNotAuthorized() {
        currentUserStore.clear()
        sessionManager.onNotAuthorized()
    }
}
