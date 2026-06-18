package com.nimboweather.forecast.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nimboweather.forecast.data.WeatherRepository
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.prefs.UnitsStore
import com.nimboweather.forecast.prefs.WidgetPrefs
import com.nimboweather.forecast.widget.WeatherWidgetProvider
import kotlin.math.roundToInt

/** Fetches weather for ONE widget's chosen city and re-renders that widget. */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt(KEY_WIDGET_ID, -1)
        if (widgetId < 0) return Result.failure()

        val prefs = WidgetPrefs(applicationContext)
        val city = prefs.getCity(widgetId) ?: return Result.success()
        val units = UnitsStore(applicationContext)

        return try {
            val cur = WeatherRepository().current(city.lat, city.lon, units.units)
            prefs.setSnapshot(
                widgetId,
                WeatherSnapshot(
                    city = city.display(),
                    temp = cur.main?.temp?.roundToInt() ?: 0,
                    symbol = units.tempSymbol(),
                    condition = cur.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                    icon = cur.weather.firstOrNull()?.icon
                )
            )
            WeatherWidgetProvider.refresh(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_WIDGET_ID = "widget_id"
    }
}
