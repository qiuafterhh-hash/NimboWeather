package com.nimboweather.forecast.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentWeather(
    val name: String? = null,
    val main: Main? = null,
    val weather: List<WeatherDesc> = emptyList(),
    val wind: Wind? = null,
    val clouds: Clouds? = null,
    val visibility: Int? = null,
    val sys: Sys? = null,
    val timezone: Int = 0
)

@Serializable
data class Main(
    val temp: Double = 0.0,
    @SerialName("feels_like") val feelsLike: Double? = null,
    val humidity: Int? = null,
    val pressure: Int? = null
)

@Serializable
data class WeatherDesc(
    val main: String? = null,
    val description: String? = null,
    val icon: String? = null
)

@Serializable
data class Wind(
    val speed: Double? = null,
    val deg: Int? = null
)

@Serializable
data class Clouds(
    val all: Int? = null
)

@Serializable
data class Sys(
    val sunrise: Long = 0L,
    val sunset: Long = 0L,
    val country: String? = null
)
