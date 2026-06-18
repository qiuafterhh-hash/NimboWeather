package com.nimboweather.forecast.ui.detail

import com.nimboweather.forecast.data.DailyForecast
import com.nimboweather.forecast.data.HourlyForecast

/** Passes full forecast lists to the detail screen (avoids re-fetch / Intent bloat). */
object DetailHolder {
    var place: String = ""
    var hourly: List<HourlyForecast> = emptyList()
    var daily: List<DailyForecast> = emptyList()
}
