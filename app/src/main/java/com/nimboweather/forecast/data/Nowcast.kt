package com.nimboweather.forecast.data

/**
 * Short-term precipitation outcome derived from a 15-minute precipitation series
 * (e.g. Open-Meteo `minutely_15`). Carries only facts — minutes until rain
 * starts/stops — so the copy layer ([Nowcast.headline]) owns wording.
 */
sealed interface NowcastState {
    /** No step crosses the rain threshold within the window. */
    object Dry : NowcastState

    /** Currently dry; rain begins in [minutesUntilStart] (a multiple of the step grain). */
    data class RainStarting(val minutesUntilStart: Int) : NowcastState

    /** Currently raining; it eases below threshold in [minutesUntilStop]. */
    data class RainStopping(val minutesUntilStop: Int) : NowcastState

    /** Currently raining with no let-up anywhere in the window. */
    object RainingThroughout : NowcastState
}

/**
 * Pure precipitation-nowcast logic. Frame-work free and deterministic — unit tested,
 * in the style of [AirQualityIndex] / [MoonPhase].
 *
 * Free data is 15-minute resolution, so reported minutes are honest multiples of the
 * step grain — never a false-precise minute count. See `docs/precip-nowcast-spec.md`.
 */
object Nowcast {

    /**
     * Reduce a precipitation series (mm per [stepMinutes] interval, index 0 = now) to a
     * [NowcastState]. A step counts as rain when its value is `>= thresholdMm`.
     */
    fun evaluate(
        precipMm: List<Double>,
        stepMinutes: Int = 15,
        thresholdMm: Double = 0.1
    ): NowcastState {
        if (precipMm.isEmpty()) return NowcastState.Dry
        val isWet = { mm: Double -> mm >= thresholdMm }

        if (!isWet(precipMm[0])) {
            val firstRain = precipMm.indexOfFirst(isWet)
            return if (firstRain < 0) NowcastState.Dry
            else NowcastState.RainStarting(firstRain * stepMinutes)
        }

        val firstDry = precipMm.indexOfFirst { !isWet(it) }
        return if (firstDry < 0) NowcastState.RainingThroughout
        else NowcastState.RainStopping(firstDry * stepMinutes)
    }

    /** Plain-language headline for a [NowcastState] (English; localized copy lives in resources). */
    fun headline(state: NowcastState): String = when (state) {
        NowcastState.Dry -> "No rain in the next hour"
        is NowcastState.RainStarting ->
            if (state.minutesUntilStart <= 15) "Rain starting within 15 min"
            else "Rain expected in about ${state.minutesUntilStart} min"
        is NowcastState.RainStopping ->
            "Rain easing — should stop within ${state.minutesUntilStop} min"
        NowcastState.RainingThroughout -> "Rain continuing for the next hour"
    }
}
