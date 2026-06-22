package com.nimboweather.forecast.data

import kotlin.math.roundToInt

/**
 * US EPA Air Quality Index computed from a PM2.5 concentration (µg/m³), plus a
 * category label. Pure, deterministic logic — unit tested.
 */
object AirQualityIndex {

    private data class Band(val cLo: Double, val cHi: Double, val aLo: Int, val aHi: Int)

    // EPA PM2.5 breakpoints (24-hour), concentration → AQI.
    private val pm25Bands = listOf(
        Band(0.0, 12.0, 0, 50),
        Band(12.1, 35.4, 51, 100),
        Band(35.5, 55.4, 101, 150),
        Band(55.5, 150.4, 151, 200),
        Band(150.5, 250.4, 201, 300),
        Band(250.5, 350.4, 301, 400),
        Band(350.5, 500.4, 401, 500)
    )

    /** US AQI (0–500) for a PM2.5 concentration in µg/m³. */
    fun usAqiFromPm25(c: Double): Int {
        if (c <= 0.0) return 0
        val b = pm25Bands.firstOrNull { c <= it.cHi } ?: return 500
        val lo = if (b.cLo == 0.0) 0.0 else b.cLo
        return ((b.aHi - b.aLo) / (b.cHi - lo) * (c - lo) + b.aLo).roundToInt()
    }

    fun category(aqi: Int): String = when {
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Unhealthy (sensitive)"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very unhealthy"
        else -> "Hazardous"
    }
}
