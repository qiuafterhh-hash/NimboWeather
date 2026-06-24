package com.nimboweather.forecast.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nimboweather.forecast.data.SnapshotBuilder
import com.nimboweather.forecast.prefs.UnitsStore
import com.nimboweather.forecast.prefs.WidgetPrefs
import com.nimboweather.forecast.widget.WeatherWidgetProvider

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
            val snap = SnapshotBuilder.build(
                city = city.display(),
                lat = city.lat,
                lon = city.lon,
                units = units.units,
                symbol = units.tempSymbol(),
                includeAqi = true,
                nowMillis = System.currentTimeMillis()
            )
            prefs.setSnapshot(widgetId, snap)
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
