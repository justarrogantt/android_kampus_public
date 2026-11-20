package com.example.ysukampus.data.session

sealed interface AuthState {
    data object Checking: AuthState
    data object Authorized : AuthState
    data object NotAuthorized : AuthState
}
