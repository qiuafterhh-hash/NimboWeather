package com.nimboweather.forecast.data.weathermap.point

import com.nimboweather.forecast.BuildConfig
import com.nimboweather.forecast.data.weathermap.WeatherLayer

/** Fetches a point forecast; network/parse failures degrade to null (caller shows nothing). */
class PointRepository(private val api: OwmPointApi = OwmPointRetrofit.api) {
    suspend fun query(
        lat: Double,
        lon: Double,
        layer: WeatherLayer,
        fallbackPlace: String
    ): PointForecast? = runCatching {
        val resp = api.current(lat = lat, lon = lon, appid = BuildConfig.OPENWEATHER_API_KEY)
        PointForecast.from(resp, layer, fallbackPlace)
    }.getOrNull()
}
