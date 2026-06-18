package com.nimboweather.forecast.ui.home

import com.nimboweather.forecast.data.DailyForecast
import com.nimboweather.forecast.data.HourlyForecast

/** Card types that can appear in the home feed (configurable order). */
enum class HomeCardType { CURRENT, HOURLY, PRECIP, DETAILS, SUNRISE_SUNSET, DAILY }

sealed interface HomeCard {
    /** Hero — rendered as the radial compass dial. */
    data class Current(
        val place: String,
        val temp: Int,
        val symbol: String,
        val desc: String,
        val icon: String?,
        val feels: Int,
        val max: Int,
        val min: Int,
        val rainProb: Int,
        val pressure: Int,
        val windText: String
    ) : HomeCard

    data class Hourly(val items: List<HourlyForecast>) : HomeCard
    data class Daily(val items: List<DailyForecast>) : HomeCard
    data class Details(val metrics: List<Metric>) : HomeCard
    data class SunriseSunset(val sunrise: String, val sunset: String) : HomeCard
    data class Precip(val points: List<PrecipPoint>) : HomeCard
}

data class Metric(val label: String, val value: String)
data class PrecipPoint(val time: String, val pop: Int)
