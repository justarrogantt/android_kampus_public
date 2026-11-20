package com.example.ysukampus.data.network.dto

import kotlinx.serialization.Serializable

/** `GET profile/joke` — случайный анекдот, не повторяющий уже показанные. */
@Serializable
data class ProfileJokeDto(
    val id: Int,
    val text: String
)
