package com.nimboweather.forecast.data.nowcast

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Second Retrofit instance — Open-Meteo lives on a different host than OpenWeatherMap,
 * so it gets its own provider (mirrors [com.nimboweather.forecast.data.RetrofitProvider]).
 */
object OpenMeteoRetrofit {
    private const val BASE_URL = "https://api.open-meteo.com/"

    private val json = Json { ignoreUnknownKeys = true }

    val api: OpenMeteoApi by lazy {
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
            .create(OpenMeteoApi::class.java)
    }
}
