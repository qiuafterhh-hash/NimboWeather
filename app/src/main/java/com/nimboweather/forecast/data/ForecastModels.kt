package com.nimboweather.forecast.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** OpenWeatherMap 5 day / 3-hour forecast (free tier). */
@Serializable
data class ForecastResponse(
    val list: List<ForecastItem> = emptyList(),
    val city: ForecastCity? = null
)

@Serializable
data class ForecastItem(
    val dt: Long = 0L,
    val main: Main? = null,
    val weather: List<WeatherDesc> = emptyList(),
    val wind: Wind? = null,
    val pop: Double = 0.0,
    @SerialName("dt_txt") val dtTxt: String? = null
)

@Serializable
data class ForecastCity(
    val name: String? = null,
    val country: String? = null
)
