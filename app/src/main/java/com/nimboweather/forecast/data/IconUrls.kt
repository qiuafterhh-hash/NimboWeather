package com.nimboweather.forecast.data

/** OpenWeatherMap weather icon CDN. */
object IconUrls {
    fun owm(icon: String?): String? =
        icon?.takeIf { it.isNotBlank() }?.let { "https://openweathermap.org/img/wn/$it@2x.png" }
}
