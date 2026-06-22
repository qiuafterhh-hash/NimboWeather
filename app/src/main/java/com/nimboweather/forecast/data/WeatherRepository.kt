package com.nimboweather.forecast.data

import com.nimboweather.forecast.BuildConfig

class WeatherRepository(
    private val api: WeatherApi = RetrofitProvider.api,
    private val apiKey: String = BuildConfig.OPENWEATHER_API_KEY
) {
    suspend fun current(lat: Double, lon: Double, units: String): CurrentWeather =
        api.current(lat = lat, lon = lon, apiKey = apiKey, units = units)

    suspend fun forecast(lat: Double, lon: Double, units: String): ForecastResponse =
        api.forecast(lat = lat, lon = lon, apiKey = apiKey, units = units)

    suspend fun airPollution(lat: Double, lon: Double): AirPollutionResponse =
        api.airPollution(lat = lat, lon = lon, apiKey = apiKey)

    suspend fun geocode(query: String): List<GeoLocation> =
        api.geocode(query = query, apiKey = apiKey, limit = 5)

    suspend fun reverse(lat: Double, lon: Double): List<GeoLocation> =
        api.reverseGeocode(lat = lat, lon = lon, apiKey = apiKey, limit = 1)
}
