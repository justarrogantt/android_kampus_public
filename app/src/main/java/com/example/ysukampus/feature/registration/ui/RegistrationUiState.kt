package com.example.ysukampus.feature.registration.ui

import com.example.ysukampus.data.network.dto.UserDto

sealed interface RegistrationUiState {
    object CheckingSession: RegistrationUiState
    object Idle: RegistrationUiState
    object DevLoading: RegistrationUiState
    object LoadingCode: RegistrationUiState
    data class DevLoginPrompt(val message: String?): RegistrationUiState
    data class WaitingConfirmation(val code: String): RegistrationUiState
    /**
     * Код подтверждён в Telegram, но юзера ещё нет в базе (бэк ответил 202).
     * Код остаётся валидным до конца TTL, поэтому поллинг продолжается и
     * подхватит регистрацию, законченную в мини-аппе.
     */
    data class NeedsRegistration(val code: String): RegistrationUiState
    data class Success(val user: UserDto): RegistrationUiState
    data class AuthorizedActions(val lastResponse: String? = null): RegistrationUiState
    data class Error(val message: String?): RegistrationUiState
}
