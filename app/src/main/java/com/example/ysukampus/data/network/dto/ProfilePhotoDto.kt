package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfilePhotoDto(
    val id: Int,
    val name: String,
    val extension: String,
    val size: Long,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("avatar_url")
    val avatarUrl: String
)
