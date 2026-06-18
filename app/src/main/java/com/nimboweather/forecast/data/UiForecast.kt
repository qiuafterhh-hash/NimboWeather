package com.nimboweather.forecast.data

/** UI-facing, unit-resolved forecast rows. */
data class HourlyForecast(
    val timeLabel: String,
    val temp: Int,
    val icon: String?
)

data class DailyForecast(
    val dayLabel: String,
    val min: Int,
    val max: Int,
    val icon: String?,
    val desc: String?
)
