package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DevLoginRequestDto(
    @SerialName("user_id")
    val userId: Int,
    val role: String,
    @SerialName("entity_id")
    val entityId: Int,
    @SerialName("first_name")
    val firstName: String
)
