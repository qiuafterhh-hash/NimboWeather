package com.nimboweather.forecast.data

import kotlin.math.PI
import kotlin.math.cos

/**
 * Moon illumination + phase name from an epoch timestamp, using the mean synodic
 * month from a known reference new moon. Pure logic — unit tested.
 */
object MoonPhase {

    private const val SYNODIC = 29.530588853
    private const val REF_NEW_MOON_MS = 947182440000L // 2000-01-06 18:14 UTC

    /** Age in days since the reference new moon, normalized to [0, SYNODIC). */
    fun ageDays(epochMs: Long): Double {
        val days = (epochMs - REF_NEW_MOON_MS) / 86_400_000.0
        return ((days % SYNODIC) + SYNODIC) % SYNODIC
    }

    /** Illuminated fraction 0..1 (0 = new moon, 1 = full moon). */
    fun illumination(epochMs: Long): Double {
        val phase = ageDays(epochMs) / SYNODIC
        return (1 - cos(2 * PI * phase)) / 2
    }

    /** True while the moon is waxing (new → full). */
    fun isWaxing(epochMs: Long): Boolean = ageDays(epochMs) < SYNODIC / 2

    fun phaseName(epochMs: Long): String {
        val a = ageDays(epochMs)
        return when {
            a < 1.0 || a > SYNODIC - 1.0 -> "New moon"
            a < SYNODIC / 4 - 1.0 -> "Waxing crescent"
            a < SYNODIC / 4 + 1.0 -> "First quarter"
            a < SYNODIC / 2 - 1.0 -> "Waxing gibbous"
            a < SYNODIC / 2 + 1.0 -> "Full moon"
            a < 3 * SYNODIC / 4 - 1.0 -> "Waning gibbous"
            a < 3 * SYNODIC / 4 + 1.0 -> "Last quarter"
            else -> "Waning crescent"
        }
    }
}
