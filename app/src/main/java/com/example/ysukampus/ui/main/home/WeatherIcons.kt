package com.example.ysukampus.ui.main.home

import androidx.annotation.DrawableRes
import com.example.ysukampus.R

/**
 * Иконка погоды по типу и времени суток. Для [WeatherType.Unknown] иконки нет —
 * тогда, как и в вебке, остаётся только текст.
 */
@DrawableRes
fun weatherIconRes(type: WeatherType, isDay: Boolean): Int? = when (type) {
    WeatherType.Clear ->
        if (isDay) R.drawable.ic_weather_clear_day else R.drawable.ic_weather_clear_night
    WeatherType.PartlyCloudy ->
        if (isDay) R.drawable.ic_weather_partly_cloudy_day else R.drawable.ic_weather_partly_cloudy_night
    WeatherType.Overcast -> R.drawable.ic_weather_overcast
    WeatherType.Fog -> R.drawable.ic_weather_fog
    WeatherType.Drizzle -> R.drawable.ic_weather_drizzle
    WeatherType.Rain -> R.drawable.ic_weather_rain
    WeatherType.FreezingRain -> R.drawable.ic_weather_freezing_rain
    WeatherType.Snow -> R.drawable.ic_weather_snow
    WeatherType.Thunderstorm -> R.drawable.ic_weather_thunderstorm
    WeatherType.Unknown -> null
}
