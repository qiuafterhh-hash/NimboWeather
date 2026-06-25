package com.nimboweather.forecast.data.weathermap.point

import retrofit2.http.GET
import retrofit2.http.Query

/** OWM Current Weather — point value at a lat/lon (free tier, consistent with the heatmap tiles). */
interface OwmPointApi {
    @GET("data/2.5/weather")
    suspend fun current(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") appid: String,
    ): OwmPointResponse
}
