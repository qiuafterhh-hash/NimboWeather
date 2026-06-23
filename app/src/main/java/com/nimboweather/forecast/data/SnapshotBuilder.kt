package com.nimboweather.forecast.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Builds an enriched [WeatherSnapshot] for the widget/notification surfaces: current
 * conditions plus next-24h hi/lo (from the forecast) and, optionally, US AQI. The
 * forecast and air-pollution calls are best-effort — if either fails, the snapshot still
 * renders with whatever the current-weather call returned (hi/lo/aqi just stay null).
 */
object SnapshotBuilder {

    private const val WINDOW_SECONDS = 24 * 60 * 60L
    private const val STRIP_SLOTS = 3

    suspend fun build(
        city: String,
        lat: Double,
        lon: Double,
        units: String,
        symbol: String,
        includeAqi: Boolean,
        nowMillis: Long,
        repo: WeatherRepository = WeatherRepository()
    ): WeatherSnapshot {
        val cur = repo.current(lat, lon, units)

        val nowSec = nowMillis / 1000
        val forecast = runCatching { repo.forecast(lat, lon, units) }.getOrNull()
        val hiLo = forecast?.let { DailyHiLo.inWindow(it.list, nowSec, nowSec + WINDOW_SECONDS) }
        val hours = forecast?.let { fc ->
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            ForecastSlots.nextSlots(fc.list, nowSec, STRIP_SLOTS).map { slot ->
                HourSlot(
                    label = fmt.format(Date(slot.dt * 1000)),
                    temp = slot.main?.temp?.roundToInt() ?: 0,
                    icon = slot.weather.firstOrNull()?.icon
                )
            }
        } ?: emptyList()

        val aqi = if (includeAqi) {
            runCatching { repo.airPollution(lat, lon) }
                .getOrNull()
                ?.list?.firstOrNull()?.components?.pm25
                ?.let { AirQualityIndex.usAqiFromPm25(it) }
        } else null

        return WeatherSnapshot(
            city = city,
            temp = cur.main?.temp?.roundToInt() ?: 0,
            symbol = symbol,
            condition = cur.weather.firstOrNull()?.description
                ?.replaceFirstChar { it.uppercase() } ?: "",
            icon = cur.weather.firstOrNull()?.icon,
            hi = hiLo?.hi,
            lo = hiLo?.lo,
            feelsLike = cur.main?.feelsLike?.roundToInt(),
            aqi = aqi,
            updatedAt = nowMillis,
            hours = hours
        )
    }
}
