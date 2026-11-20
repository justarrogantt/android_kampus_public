package com.example.ysukampus.data.weather

import com.example.ysukampus.data.auth.AuthorizedService
import com.example.ysukampus.data.network.dto.WeatherDto
import com.example.ysukampus.ui.main.home.Weather
import com.example.ysukampus.ui.main.home.WeatherType
import java.io.IOException
import retrofit2.HttpException

/**
 * Погода для шапки главной. Возвращает [Result] — как и остальные репозитории,
 * чтобы ViewModel не знала про Retrofit.
 */
class WeatherRepository(
    private val authorizedService: AuthorizedService
) {
    suspend fun getWeather(): Result<Weather> = try {
        Result.success(authorizedService.getWeather().toDomain())
    } catch (error: IOException) {
        Result.failure(WeatherException(ERROR_MESSAGE, error))
    } catch (error: HttpException) {
        Result.failure(WeatherException(ERROR_MESSAGE, error))
    }

    private companion object {
        const val ERROR_MESSAGE = "Не удалось загрузить погоду"
    }
}

/** Ошибка с готовым для показа текстом. */
class WeatherException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

internal fun WeatherDto.toDomain(): Weather = Weather(
    city = city,
    temperature = temperature,
    condition = condition,
    type = WeatherType.fromApi(type),
    isDay = isDay
)
