package com.example.ysukampus.feature.registration.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.auth.AuthStartResult
import com.example.ysukampus.data.session.AuthConfirmationResult
import com.example.ysukampus.feature.registration.domain.RegistrationInteractor
import com.example.ysukampus.data.network.error.toReadableMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val interactor: RegistrationInteractor
) : ViewModel() {
    private companion object {
        const val POST_LOGOUT_DEMO_DELAY_MS = 2000L
        const val POLLING_INTERVAL_MS = 3000L

        // Статусы кода входа, которые отдаёт GET auth/status/{code}.
        // Код живёт как PENDING -> SUCCESS либо, если юзера ещё нет в базе,
        // PENDING -> NEEDS_REGISTRATION -> SUCCESS.
        const val STATUS_PENDING = "pending"
        const val STATUS_NEEDS_REGISTRATION = "needs_registration"
        const val STATUS_SUCCESS = "success"
    }

    var registrationUiState: RegistrationUiState by mutableStateOf(RegistrationUiState.CheckingSession)
        private set

    private var pollingJob: Job? = null

    fun onAppStarted() {
        viewModelScope.launch {
            val hasSession = interactor.restoreAuthorizedSession()
            if (hasSession) {
                registrationUiState = RegistrationUiState.AuthorizedActions()
            } else {
                beginAuthorization()
            }
        }
    }

    private fun beginAuthorization() {
        viewModelScope.launch {
            registrationUiState = if (interactor.isDevAuthMode()) {
                RegistrationUiState.DevLoading
            } else {
                RegistrationUiState.LoadingCode
            }
            try {
                when (val result = interactor.beginAuthorization()) {
                    is AuthStartResult.DevAuthorized -> {
                        interactor.saveCurrentUser(result.response.user)
                        interactor.onAuthorized()
                        registrationUiState = RegistrationUiState.AuthorizedActions(
                            "Выполнен dev-вход как ${result.response.user.firstName}"
                        )
                    }
                    is AuthStartResult.TelegramCode -> {
                        val code = result.code.trim()
                        registrationUiState = RegistrationUiState.WaitingConfirmation(code)
                        startStatusPolling(code)
                    }
                }
            } catch (e: Exception) {
                Log.e("RegistrationViewModel", "Failed to start authorization", e)
                registrationUiState = if (interactor.isDevAuthMode()) {
                    RegistrationUiState.DevLoginPrompt(e.toReadableMessage())
                } else {
                    RegistrationUiState.Error(e.toReadableMessage())
                }
            }
        }
    }

    private fun startStatusPolling(
        code: String
    ) {
        val normalizedCode = code.trim()
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    when (val status = interactor.getAuthentificationStatus(normalizedCode)) {
                        STATUS_PENDING -> {
                            delay(POLLING_INTERVAL_MS)
                        }
                        STATUS_NEEDS_REGISTRATION -> {
                            // Бот увидел код, но юзера ещё нет в базе и отправил
                            // его регистрироваться в мини-апп. Код жив до конца
                            // TTL, поэтому продолжаем опрашивать: регистрация
                            // сама переведёт его в success.
                            registrationUiState = RegistrationUiState.NeedsRegistration(normalizedCode)
                            delay(POLLING_INTERVAL_MS)
                        }
                        STATUS_SUCCESS -> {
                            when (val confirmationResult = interactor.confirmAuthentification(normalizedCode)) {
                                is AuthConfirmationResult.Success -> {
                                    interactor.saveCurrentUser(confirmationResult.response.user)
                                    interactor.onAuthorized()
                                    registrationUiState = RegistrationUiState.Success(confirmationResult.response.user)
                                    pollingJob = null
                                    break
                                }
                                AuthConfirmationResult.NeedsRegistration -> {
                                    // 202: код подтверждён, но юзера в базе нет. Бэк в этой
                                    // ветке код не гасит, поэтому продолжаем опрашивать —
                                    // как только регистрация в мини-аппе закончится, тот же
                                    // код обменяется на токены. Выход из цикла даёт TTL:
                                    // протухший код обрушит getAuthentificationStatus в 404.
                                    registrationUiState = RegistrationUiState.NeedsRegistration(normalizedCode)
                                    delay(POLLING_INTERVAL_MS)
                                }
                            }
                        }
                        else -> {
                            registrationUiState =
                                RegistrationUiState.Error("Неизвестный статус кода входа: $status")
                            pollingJob = null
                            break
                        }
                    }
                } catch (e: CancellationException) {
                    // Отмену пробрасываем: её кидает stopStatusPolling() при запросе
                    // нового кода и при выходе. Если проглотить — отменённый цикл
                    // перетрёт свежее состояние экраном ошибки.
                    throw e
                } catch (e: Exception) {
                    Log.e("RegistrationViewModel", "Authentification polling failed", e)
                    registrationUiState = RegistrationUiState.Error(e.toReadableMessage())
                    pollingJob = null
                    break
                }
            }
        }
    }

    private fun stopStatusPolling(){
        pollingJob?.cancel()
        pollingJob = null
    }

    fun getAuthentificationCode() {
        stopStatusPolling()
        beginAuthorization()
    }

    fun onRetryRegistration() {
        stopStatusPolling()
        beginAuthorization()
    }

    fun onOpenScheduleClick() {
        registrationUiState = RegistrationUiState.AuthorizedActions()
    }

    fun onSessionExpired() {
        stopStatusPolling()
        interactor.onNotAuthorized()
        beginAuthorization()
    }

    fun getTokens() {
        viewModelScope.launch {
            try {
                registrationUiState = RegistrationUiState.AuthorizedActions(interactor.getSessionsSummary())
            } catch (e: Exception) {
                registrationUiState = RegistrationUiState.AuthorizedActions(e.toReadableMessage())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val response = interactor.logout()
                registrationUiState = RegistrationUiState.AuthorizedActions(response)
                delay(POST_LOGOUT_DEMO_DELAY_MS)
                stopStatusPolling()
                beginAuthorization()
            } catch (e: Exception) {
                registrationUiState = RegistrationUiState.AuthorizedActions(e.toReadableMessage())
            }
        }
    }

    fun logoutAll() {
        viewModelScope.launch {
            try {
                val response = interactor.logoutAll()
                registrationUiState = RegistrationUiState.AuthorizedActions(response)
                delay(POST_LOGOUT_DEMO_DELAY_MS)
                stopStatusPolling()
                beginAuthorization()
            } catch (e: Exception) {
                registrationUiState = RegistrationUiState.AuthorizedActions(e.toReadableMessage())
            }
        }
    }
}
