package com.nimboweather.forecast.data.weathermap.point

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OwmPointResponse(
    val name: String = "",
    val dt: Long = 0L,
    val main: OwmMain = OwmMain(),
    val wind: OwmWind = OwmWind(),
    val rain: OwmRain? = null,
)

@Serializable
data class OwmMain(val temp: Double = 0.0)

@Serializable
data class OwmWind(val speed: Double = 0.0)

@Serializable
data class OwmRain(@SerialName("1h") val oneHour: Double = 0.0)
