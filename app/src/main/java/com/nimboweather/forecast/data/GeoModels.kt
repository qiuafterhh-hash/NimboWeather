package com.nimboweather.forecast.data

import kotlinx.serialization.Serializable

/** OpenWeatherMap Geocoding API result. */
@Serializable
data class GeoLocation(
    val name: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val country: String? = null,
    val state: String? = null
) {
    fun display(): String = listOfNotNull(name, state, country).joinToString(", ")
}
