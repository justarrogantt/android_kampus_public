package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NeedsRegistrationResponse(
    @SerialName("needs_registration")
    val needsRegistration: Boolean
)
