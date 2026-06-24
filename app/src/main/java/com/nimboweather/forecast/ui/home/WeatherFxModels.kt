package com.nimboweather.forecast.ui.home

import kotlin.math.sin

/** Animated background scene, derived from the OWM icon code + day/night suffix. */
enum class FxScene { NONE, CLEAR_DAY, CLEAR_NIGHT, CLOUDS, OVERCAST, RAIN, STORM, SNOW, FOG }

/** Immutable params driving [WeatherFxView]; built from the home cards. */
data class FxSpec(
    val scene: FxScene,
    val windDeg: Int? = null,
    val windSpeed: Float = 0f,   // m/s
    val intensity: Float = 0.5f  // 0..1
)

/** Framework-free mapping helpers — unit-tested like AirQualityIndex / MoonPhase. */
object FxMapper {

    fun sceneFrom(icon: String?): FxScene {
        val code = icon?.take(2) ?: return FxScene.NONE
        val night = icon.endsWith("n")
        return when (code) {
            "11" -> FxScene.STORM
            "09", "10" -> FxScene.RAIN
            "13" -> FxScene.SNOW
            "50" -> FxScene.FOG
            "01" -> if (night) FxScene.CLEAR_NIGHT else FxScene.CLEAR_DAY
            "02" -> if (night) FxScene.CLEAR_NIGHT else FxScene.CLOUDS
            "03" -> FxScene.CLOUDS
            "04" -> FxScene.OVERCAST
            else -> FxScene.NONE
        }
    }

    /** 0..1 intensity. Priority: nowcast mm peak → rainProb → icon code. */
    fun intensityFrom(nowcastSeries: List<Double>?, rainProb: Int?, icon: String?): Float {
        val peak = nowcastSeries?.maxOrNull()
        if (peak != null && peak > 0.0) {
            // ~6 mm per 15-min slot ≈ full intensity; floor at 0.2 so any rain shows.
            return (0.2f + (peak / 6.0).toFloat()).coerceIn(0.2f, 1f)
        }
        if (rainProb != null && rainProb > 0) {
            return (rainProb / 100f).coerceIn(0.1f, 1f)
        }
        return when (icon?.take(2)) {
            "11" -> 0.85f
            "10" -> 0.6f
            "09" -> 0.5f
            "13" -> 0.5f
            else -> 0.4f
        }
    }

    /**
     * Horizontal drift as a fraction of vertical speed.
     * windDeg is meteorological (direction wind blows FROM); + = drift right (eastward).
     * Capped at ±0.6.
     */
    fun tilt(windDeg: Int?, windSpeed: Float): Float {
        if (windDeg == null || windSpeed <= 0f) return 0f
        val rad = Math.toRadians(windDeg.toDouble())
        val eastward = (-sin(rad)).toFloat()            // wind FROM west(270°) → +1 (toward east)
        val strength = (windSpeed / 12f).coerceIn(0f, 1f)
        return eastward * strength * 0.6f
    }
}
