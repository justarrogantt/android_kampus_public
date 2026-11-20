package com.example.ysukampus.data.session

import com.example.ysukampus.data.network.dto.AuthentificationConfirmedResponse

sealed interface AuthConfirmationResult {
    data class Success(val response: AuthentificationConfirmedResponse) : AuthConfirmationResult
    object NeedsRegistration : AuthConfirmationResult
}
