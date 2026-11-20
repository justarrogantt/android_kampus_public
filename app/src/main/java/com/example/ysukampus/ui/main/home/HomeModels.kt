package com.example.ysukampus.ui.main.home

/** Тип погоды: машинный ключ бэка под иконку. Незнакомое значение → [Unknown]. */
enum class WeatherType {
    Clear,
    PartlyCloudy,
    Overcast,
    Fog,
    Drizzle,
    Rain,
    FreezingRain,
    Snow,
    Thunderstorm,
    Unknown;

    companion object {
        fun fromApi(raw: String): WeatherType = when (raw) {
            "clear" -> Clear
            "partly_cloudy" -> PartlyCloudy
            "overcast" -> Overcast
            "fog" -> Fog
            "drizzle" -> Drizzle
            "rain" -> Rain
            "freezing_rain" -> FreezingRain
            "snow" -> Snow
            "thunderstorm" -> Thunderstorm
            else -> Unknown
        }
    }
}

/** Текущая погода города филиала. */
data class Weather(
    val city: String,
    val temperature: Int,
    val condition: String,
    val type: WeatherType,
    val isDay: Boolean
)
