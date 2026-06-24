package com.nimboweather.forecast.data.nowcast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open-Meteo forecast response, narrowed to the 15-minute precipitation block.
 * Free, global, no API key — the nowcast data source (see `docs/precip-nowcast-spec.md`).
 *
 * Open-Meteo may emit `null` inside value arrays where a point has no data, hence the
 * nullable element type; [OpenMeteoMapper] normalizes those to 0.0.
 */
@Serializable
data class OpenMeteoResponse(
    @SerialName("minutely_15") val minutely15: Minutely15? = null
)

@Serializable
data class Minutely15(
    val time: List<String> = emptyList(),
    val precipitation: List<Double?> = emptyList()
)
