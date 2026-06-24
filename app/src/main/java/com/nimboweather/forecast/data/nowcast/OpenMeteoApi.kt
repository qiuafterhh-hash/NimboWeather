package com.nimboweather.forecast.data.nowcast

import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo forecast endpoint, scoped to the 15-minute precipitation block. */
interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun minutely15(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("minutely_15") fields: String = "precipitation",
        @Query("forecast_minutely_15") steps: Int = 8,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
