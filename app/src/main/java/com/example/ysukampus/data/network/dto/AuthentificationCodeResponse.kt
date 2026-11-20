package com.example.ysukampus.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthentificationCodeResponse(
    val code: String
)
