package com.nimboweather.forecast.data.nowcast

import com.nimboweather.forecast.data.Nowcast
import com.nimboweather.forecast.data.NowcastState

/** Nowcast outcome: the [state] (for headline + push) plus the raw [series] (for the curve). */
data class NowcastResult(val state: NowcastState, val series: List<Double>)

/**
 * Fetches the 15-minute precipitation series from Open-Meteo and reduces it to a
 * [NowcastResult] via the pure [Nowcast] logic. Network failures degrade to an empty
 * result rather than surfacing — a missing nowcast should never break the home screen.
 */
class NowcastRepository(
    private val api: OpenMeteoApi = OpenMeteoRetrofit.api
) {
    suspend fun nowcast(lat: Double, lon: Double): NowcastResult =
        runCatching {
            val response = api.minutely15(lat = lat, lon = lon)
            val series = OpenMeteoMapper.precipSeries(response)
            NowcastResult(Nowcast.evaluate(series), series)
        }.getOrDefault(NowcastResult(NowcastState.Dry, emptyList()))
}
