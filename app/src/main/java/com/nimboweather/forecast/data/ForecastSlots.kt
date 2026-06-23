package com.nimboweather.forecast.data

/**
 * Picks the next N forecast slots at/after a reference time, for the widget's short
 * 3-hour strip. Pure, deterministic — unit tested.
 */
object ForecastSlots {

    /** First [count] items with `dt` >= [nowEpochSec], ordered by time. */
    fun nextSlots(items: List<ForecastItem>, nowEpochSec: Long, count: Int): List<ForecastItem> =
        items.filter { it.dt >= nowEpochSec }
            .sortedBy { it.dt }
            .take(count)
}
