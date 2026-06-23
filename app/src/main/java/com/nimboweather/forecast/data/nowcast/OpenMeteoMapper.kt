package com.nimboweather.forecast.data.nowcast

/**
 * Pure mapping from an [OpenMeteoResponse] to the precipitation series (mm) that
 * `Nowcast.evaluate` consumes. Frame-work free and unit tested.
 */
object OpenMeteoMapper {

    /**
     * Precipitation values (mm) for the next [maxSteps] 15-minute steps. A missing block or
     * `null` entries normalize to an empty list / 0.0 so downstream nowcast degrades safely.
     */
    fun precipSeries(response: OpenMeteoResponse, maxSteps: Int = 8): List<Double> =
        response.minutely15?.precipitation.orEmpty()
            .take(maxSteps)
            .map { it ?: 0.0 }
}
