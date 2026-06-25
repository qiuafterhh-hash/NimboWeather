package com.nimboweather.forecast.data.weathermap.point

import com.nimboweather.forecast.data.weathermap.WeatherLayer
import kotlin.math.roundToInt

/** A resolved popup: a place name and the value of the active layer at that point. */
data class PointForecast(val place: String, val value: String) {
    companion object {
        fun from(
            resp: OwmPointResponse,
            layer: WeatherLayer,
            fallbackPlace: String = ""
        ): PointForecast {
            val place = resp.name.ifBlank { fallbackPlace }
            val value = when (layer) {
                WeatherLayer.WIND -> "${resp.wind.speed.roundToInt()} m/s"
                WeatherLayer.PRECIP, WeatherLayer.RADAR -> {
                    val mm = resp.rain?.oneHour ?: 0.0
                    if (mm == 0.0) "0 mm" else String.format(java.util.Locale.US, "%.1f mm", mm)
                }
                WeatherLayer.TEMP -> "${resp.main.temp.roundToInt()}°C"
            }
            return PointForecast(place, value)
        }
    }
}
