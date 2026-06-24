package com.nimboweather.forecast.prefs

import android.content.Context
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.widget.WidgetAppearance
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Per-widget config: which city each widget tracks + its last weather snapshot + appearance. */
class WidgetPrefs(context: Context) {
    private val sp = context.getSharedPreferences(UnitsStore.PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun setCity(widgetId: Int, city: SavedCity) {
        sp.edit().putString("w_city_$widgetId", json.encodeToString(city)).apply()
    }

    fun getCity(widgetId: Int): SavedCity? =
        sp.getString("w_city_$widgetId", null)?.let {
            runCatching { json.decodeFromString<SavedCity>(it) }.getOrNull()
        }

    fun setSnapshot(widgetId: Int, snap: WeatherSnapshot) {
        sp.edit().putString("w_snap_$widgetId", json.encodeToString(snap)).apply()
    }

    fun getSnapshot(widgetId: Int): WeatherSnapshot? =
        sp.getString("w_snap_$widgetId", null)?.let {
            runCatching { json.decodeFromString<WeatherSnapshot>(it) }.getOrNull()
        }

    /** Per-widget appearance (style + background opacity + theme). Defaults when unset. */
    fun setAppearance(widgetId: Int, appearance: WidgetAppearance) {
        sp.edit().putString("w_appr_$widgetId", json.encodeToString(appearance)).apply()
    }

    fun getAppearance(widgetId: Int): WidgetAppearance =
        sp.getString("w_appr_$widgetId", null)
            ?.let { runCatching { json.decodeFromString<WidgetAppearance>(it) }.getOrNull() }
            ?: WidgetAppearance()

    fun clear(widgetId: Int) {
        sp.edit()
            .remove("w_city_$widgetId")
            .remove("w_snap_$widgetId")
            .remove("w_appr_$widgetId")
            .apply()
    }
}
