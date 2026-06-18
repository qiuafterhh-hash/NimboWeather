package com.nimboweather.forecast.data

import android.content.Context
import com.nimboweather.forecast.prefs.UnitsStore

/** Last current-weather snapshot, used by the widget + notification (instant render). */
data class WeatherSnapshot(
    val city: String,
    val temp: Int,
    val symbol: String,
    val condition: String,
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
            .apply()
    }

    fun load(): WeatherSnapshot = WeatherSnapshot(
        city = sp.getString(K_CITY, "—") ?: "—",
        temp = sp.getInt(K_TEMP, 0),
        symbol = sp.getString(K_SYM, "°") ?: "°",
        condition = sp.getString(K_COND, "") ?: "",
        icon = sp.getString(K_ICON, null)
    )

    fun hasData(): Boolean = sp.contains(K_CITY)

    companion object {
        private const val K_CITY = "snap_city"
        private const val K_TEMP = "snap_temp"
        private const val K_SYM = "snap_sym"
        private const val K_COND = "snap_cond"
        private const val K_ICON = "snap_icon"
    }
}
