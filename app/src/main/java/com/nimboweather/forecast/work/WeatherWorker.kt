package com.nimboweather.forecast.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nimboweather.forecast.data.DailyHiLo
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.data.WeatherRepository
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.notify.Notifications
import com.nimboweather.forecast.prefs.AppPrefs
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
            val repo = WeatherRepository()
            val cur = repo.current(lat, lon, units.units)
            val now = System.currentTimeMillis()
            val hiLo = runCatching { repo.forecast(lat, lon, units.units) }
                .getOrNull()
                ?.let { DailyHiLo.inWindow(it.list, now / 1000, now / 1000 + 24 * 60 * 60L) }
            val snap = WeatherSnapshot(
                city = place,
                temp = cur.main?.temp?.roundToInt() ?: 0,
                symbol = units.tempSymbol(),
                condition = cur.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                icon = cur.weather.firstOrNull()?.icon,
                hi = hiLo?.hi,
                lo = hiLo?.lo,
                feelsLike = cur.main?.feelsLike?.roundToInt(),
                updatedAt = now
            )
            WeatherCache(applicationContext).save(snap)

            val prefs = AppPrefs(applicationContext)
            if (prefs.persistentNotification) {
                Notifications.updatePersistent(applicationContext, snap, true)
            }
            Notifications.postDaily(applicationContext, snap)

            val condId = cur.weather.firstOrNull()?.id ?: 0
            if (isSevere(condId)) {
                val what = cur.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Severe conditions"
                Notifications.postAlert(applicationContext, "Severe weather alert", "$what in $place")
            }

            // Re-render fallback widgets now, and re-fetch each configured widget's
            // own city so they update on this periodic cadence (not just hourly onUpdate).
            WeatherWidgetProvider.refresh(applicationContext)
            WeatherWidgetProvider.enqueueRefreshAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val DEFAULT_LAT = 51.5074
        private const val DEFAULT_LON = -0.1278

        // OWM condition codes: 2xx thunderstorm, 781 tornado, 504 extreme heat,
        // 602 heavy snow, 622 heavy shower snow.
        private fun isSevere(id: Int): Boolean =
            id in 200..232 || id == 781 || id == 504 || id == 602 || id == 622
    }
}
