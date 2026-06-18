package com.nimboweather.forecast.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.data.WeatherRepository
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.notify.Notifications
import com.nimboweather.forecast.prefs.CityStore
import com.nimboweather.forecast.prefs.UnitsStore
import com.nimboweather.forecast.widget.WeatherWidgetProvider
import kotlin.math.roundToInt

/** Periodic background refresh: fetch current weather for the selected city,
 *  cache it, post a notification, and refresh the home widget. */
class WeatherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cityStore = CityStore(applicationContext)
        val units = UnitsStore(applicationContext)
        val target = cityStore.selected

        val lat = target?.lat ?: DEFAULT_LAT
        val lon = target?.lon ?: DEFAULT_LON
        val place = target?.display() ?: "London"

        return try {
            val cur = WeatherRepository().current(lat, lon, units.units)
            val snap = WeatherSnapshot(
                city = place,
                temp = cur.main?.temp?.roundToInt() ?: 0,
                symbol = units.tempSymbol(),
                condition = cur.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                icon = cur.weather.firstOrNull()?.icon
            )
            WeatherCache(applicationContext).save(snap)
            Notifications.postDaily(applicationContext, snap)
            WeatherWidgetProvider.refresh(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val DEFAULT_LAT = 51.5074
        private const val DEFAULT_LON = -0.1278
    }
}
