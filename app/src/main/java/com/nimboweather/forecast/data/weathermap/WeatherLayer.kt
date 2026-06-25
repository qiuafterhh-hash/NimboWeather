package com.nimboweather.forecast.data.weathermap

import androidx.annotation.StringRes
import com.nimboweather.forecast.R

/**
 * A selectable weather-map layer. [owmLayer] is the OpenWeatherMap tile layer id for model-based
 * layers (global coverage); RADAR has none — it is sourced from coverage-gated NEXRAD tiles and
 * falls back to PRECIP outside the covered region. [scaleMin]/[scaleMax]/[scaleColors] describe the
 * legend gradient only (the tile pixels are colored by the provider).
 */
enum class WeatherLayer(
    @StringRes val labelRes: Int,
    val owmLayer: String?,
    val scaleMin: Int,
    val scaleMax: Int,
    val scaleUnit: String,
    val scaleColors: IntArray,
) {
    TEMP(
        R.string.layer_temp, "temp_new", -20, 40, "°C",
        intArrayOf(0xFF4A2DB5.toInt(), 0xFF2E9BE6.toInt(), 0xFF49D49D.toInt(),
            0xFFE8E84A.toInt(), 0xFFE8862E.toInt(), 0xFFD53A2D.toInt())
    ),
    PRECIP(
        R.string.layer_precip, "precipitation_new", 0, 50, "mm",
        intArrayOf(0x00FFFFFF, 0xFF9BD3F0.toInt(), 0xFF3A86E8.toInt(), 0xFF6B3AE8.toInt())
    ),
    WIND(
        R.string.layer_wind, "wind_new", 0, 30, "m/s",
        intArrayOf(0xFF49D49D.toInt(), 0xFFE8E84A.toInt(), 0xFFE8862E.toInt(), 0xFFD53A2D.toInt())
    ),
    RADAR(
        R.string.layer_radar, null, 0, 70, "dBZ",
        intArrayOf(0xFF49D49D.toInt(), 0xFFE8E84A.toInt(), 0xFFE8862E.toInt(), 0xFFD53A2D.toInt())
    );
}
