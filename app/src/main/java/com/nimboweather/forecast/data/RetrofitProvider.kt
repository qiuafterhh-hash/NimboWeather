package com.nimboweather.forecast.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object RetrofitProvider {
    private const val BASE_URL = "https://api.openweathermap.org/"

    private val json = Json { ignoreUnknownKeys = true }

    /** Shared Retrofit for the OpenWeatherMap host; reuse this for any OWM endpoint. */
    val retrofit: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val api: WeatherApi by lazy { retrofit.create(WeatherApi::class.java) }
}
