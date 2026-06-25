package com.nimboweather.forecast.data.weathermap.point

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object OwmPointRetrofit {
    private const val BASE_URL = "https://api.openweathermap.org/"
    private val json = Json { ignoreUnknownKeys = true }

    val api: OwmPointApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OwmPointApi::class.java)
    }
}
