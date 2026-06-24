package com.nimboweather.forecast.data

import kotlin.math.roundToInt

/**
 * High/low temperature aggregated from the 5-day/3-hour forecast over a time window.
 * Window-based (not calendar-date) so it stays timezone-safe for remote cities — the
 * caller passes a rolling next-24h window. Pure, deterministic logic — unit tested.
 */
object DailyHiLo {

    data class HiLo(val hi: Int, val lo: Int)

    /**
     * Hi/Lo across forecast items whose `dt` (epoch seconds) is in [startEpochSec, endEpochSec).
     * Items without a `main` temperature are ignored. Returns null if no item qualifies.
     */
    fun inWindow(items: List<ForecastItem>, startEpochSec: Long, endEpochSec: Long): HiLo? {
        val temps = items
            .filter { it.dt in startEpochSec until endEpochSec && it.main != null }
            .map { it.main!!.temp }
        if (temps.isEmpty()) return null
        return HiLo(hi = temps.max().roundToInt(), lo = temps.min().roundToInt())
    }
}
