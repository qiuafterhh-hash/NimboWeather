package com.nimboweather.forecast.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.data.WeatherRepository
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.data.nowcast.NowcastAlerts
import com.nimboweather.forecast.data.nowcast.NowcastRepository
import com.nimboweather.forecast.notify.Notifications
import com.nimboweather.forecast.prefs.AppPrefs
import com.nimboweather.forecast.prefs.CityStore
import com.nimboweather.forecast.prefs.UnitsStore
import com.nimboweather.forecast.widget.WeatherWidgetProvider
import java.util.Calendar
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

            maybeNotifyNowcast(prefs, lat, lon)

            WeatherWidgetProvider.refresh(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /** Fetch the rain nowcast for the active location and post a heads-up if one is due. */
    private suspend fun maybeNotifyNowcast(prefs: AppPrefs, lat: Double, lon: Double) {
        val result = NowcastRepository().nowcast(lat, lon)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val decision = NowcastAlerts.decide(result.state, prefs.nowcastEventKey, hour)
        prefs.nowcastEventKey = decision.eventKey
        decision.notification?.let {
            Notifications.postNowcast(applicationContext, it.title, it.body)
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
