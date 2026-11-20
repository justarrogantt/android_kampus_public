package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetTokensResponse(
    val tokens: List<TokensDTO>
)

@Serializable
data class TokensDTO(
    val id: String,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("device_info")
    val deviceInfo: String,
)
