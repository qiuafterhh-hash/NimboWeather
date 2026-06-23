package com.nimboweather.forecast.data

import android.content.Context
import com.nimboweather.forecast.prefs.UnitsStore
import kotlinx.serialization.Serializable

/** Last current-weather snapshot, used by the widget + notification (instant render). */
@Serializable
data class WeatherSnapshot(
    val city: String,
    val temp: Int,
    val symbol: String,
    val condition: String,
    val icon: String?,
    val hi: Int? = null,         // daily/next-24h high, from forecast aggregation
    val lo: Int? = null,         // daily/next-24h low
    val feelsLike: Int? = null,  // CurrentWeather.main.feelsLike, rounded
    val aqi: Int? = null,        // US AQI (large widget only)
    val updatedAt: Long? = null, // epoch millis of fetch, for "updated Xm ago" + staleness
    val hours: List<HourSlot> = emptyList() // next few 3-hour slots (large widget strip)
)

/** One slot in the widget's short forecast strip. `label` is pre-formatted (e.g. "15:00"). */
@Serializable
data class HourSlot(
    val label: String,
    val temp: Int,
    val icon: String?
)

class WeatherCache(context: Context) {
    private val sp = context.getSharedPreferences(UnitsStore.PREFS, Context.MODE_PRIVATE)

    fun save(s: WeatherSnapshot) {
        sp.edit()
            .putString(K_CITY, s.city)
            .putInt(K_TEMP, s.temp)
            .putString(K_SYM, s.symbol)
            .putString(K_COND, s.condition)
            .putString(K_ICON, s.icon)
            .putInt(K_HI, s.hi ?: ABSENT)
            .putInt(K_LO, s.lo ?: ABSENT)
            .putInt(K_FEELS, s.feelsLike ?: ABSENT)
            .putInt(K_AQI, s.aqi ?: ABSENT)
            .putLong(K_UPDATED, s.updatedAt ?: 0L)
            .apply()
    }

    fun load(): WeatherSnapshot = WeatherSnapshot(
        city = sp.getString(K_CITY, "—") ?: "—",
        temp = sp.getInt(K_TEMP, 0),
        symbol = sp.getString(K_SYM, "°") ?: "°",
        condition = sp.getString(K_COND, "") ?: "",
        icon = sp.getString(K_ICON, null),
        hi = sp.getInt(K_HI, ABSENT).takeIf { it != ABSENT },
        lo = sp.getInt(K_LO, ABSENT).takeIf { it != ABSENT },
        feelsLike = sp.getInt(K_FEELS, ABSENT).takeIf { it != ABSENT },
        aqi = sp.getInt(K_AQI, ABSENT).takeIf { it != ABSENT },
        updatedAt = sp.getLong(K_UPDATED, 0L).takeIf { it != 0L }
    )

    fun hasData(): Boolean = sp.contains(K_CITY)

    companion object {
        // Sentinel for absent nullable Int fields (outside any realistic temp/AQI range).
        private const val ABSENT = Int.MIN_VALUE
        private const val K_CITY = "snap_city"
        private const val K_TEMP = "snap_temp"
        private const val K_SYM = "snap_sym"
        private const val K_COND = "snap_cond"
        private const val K_ICON = "snap_icon"
        private const val K_HI = "snap_hi"
        private const val K_LO = "snap_lo"
        private const val K_FEELS = "snap_feels"
        private const val K_AQI = "snap_aqi"
        private const val K_UPDATED = "snap_updated"
    }
}
