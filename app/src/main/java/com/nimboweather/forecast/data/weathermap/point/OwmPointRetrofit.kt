package com.nimboweather.forecast.data.weathermap.point

import com.nimboweather.forecast.data.RetrofitProvider

/**
 * OWM Current Weather lives on the same host as [RetrofitProvider], so it reuses that shared
 * Retrofit (one OkHttp pool + the debug logging interceptor) rather than building its own.
 */
object OwmPointRetrofit {
    val api: OwmPointApi by lazy { RetrofitProvider.retrofit.create(OwmPointApi::class.java) }
}
