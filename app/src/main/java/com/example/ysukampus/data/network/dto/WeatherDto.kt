package com.example.ysukampus.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Зеркало `WeatherResponse` из `app/schemas/weather_schemas.py`.
 *
 * `type` — машинный ключ погоды под иконку; набор задаётся бэком, поэтому в
 * домене неизвестное значение деградирует в `WeatherType.Unknown`, а не роняет
 * главную (так же, как `.catch("unknown")` в вебке).
 */
@Serializable
data class WeatherDto(
    val city: String,
    val temperature: Int,
    val condition: String,
    val type: String,
    @SerialName("is_day")
    val isDay: Boolean,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val stale: Boolean = false
)
