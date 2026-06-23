package com.nimboweather.forecast.data.radar

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET

/** RainViewer public maps index — the list of available radar frames + tile host. */
interface RainViewerApi {
    @GET("public/weather-maps.json")
    suspend fun maps(): RainViewerMaps
}

/** Own Retrofit instance — RainViewer is a third host (mirrors the OWM / Open-Meteo providers). */
object RainViewerRetrofit {
    private const val BASE_URL = "https://api.rainviewer.com/"
    private val json = Json { ignoreUnknownKeys = true }

    val api: RainViewerApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RainViewerApi::class.java)
    }
}

/** Fetches the radar frame timeline; network failures degrade to an empty list. */
class RadarRepository(private val api: RainViewerApi = RainViewerRetrofit.api) {
    suspend fun load(): RadarTimeline =
        runCatching {
            val maps = api.maps()
            RadarTimeline(host = maps.host, frames = RainViewerTiles.frames(maps))
        }.getOrDefault(RadarTimeline("", emptyList()))
}

/** Resolved radar timeline: the tile [host] and ordered [frames] ready to animate. */
data class RadarTimeline(val host: String, val frames: List<RadarFrame>)
