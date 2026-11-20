package com.example.ysukampus.data.session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager(
    private val tokenStorage: TokenStorage
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState

    suspend fun restoreSession() {
        val access = tokenStorage.getAccessToken()
        val refresh = tokenStorage.getRefreshToken()

        _authState.value =
            if (access != null && refresh != null) AuthState.Authorized
            else AuthState.NotAuthorized
    }

    fun onAuthorized() {
        _authState.value = AuthState.Authorized
    }

    fun onNotAuthorized() {
        tokenStorage.clearTokens()
        _authState.value = AuthState.NotAuthorized
    }
}
