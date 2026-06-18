package com.nimboweather.forecast.prefs

import android.content.Context

/** Persisted unit system. metric = °C / m/s, imperial = °F / mph. */
class UnitsStore(context: Context) {
    private val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var units: String
        get() = sp.getString(KEY, "metric") ?: "metric"
        set(value) { sp.edit().putString(KEY, value).apply() }

    fun isMetric(): Boolean = units == "metric"
    fun toggle() { units = if (isMetric()) "imperial" else "metric" }
    fun tempSymbol(): String = if (isMetric()) "°C" else "°F"
    fun speedSymbol(): String = if (isMetric()) "m/s" else "mph"

    companion object {
        const val PREFS = "nimbo_prefs"
        private const val KEY = "units"
    }
}
