package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequestDTO(
    @SerialName("refresh_token")
    val refreshToken: String
)
