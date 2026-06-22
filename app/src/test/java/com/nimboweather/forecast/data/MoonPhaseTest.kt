package com.nimboweather.forecast.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonPhaseTest {

    private val refNewMoon = 947182440000L          // 2000-01-06 18:14 UTC (new moon)
    private val halfSynodicMs = 1_275_721_438L      // ~14.765 days → full moon

    @Test fun new_moon_is_dark() {
        assertEquals(0.0, MoonPhase.illumination(refNewMoon), 0.03)
    }

    @Test fun full_moon_is_lit() {
        assertEquals(1.0, MoonPhase.illumination(refNewMoon + halfSynodicMs), 0.03)
    }

    @Test fun new_moon_named() {
        assertEquals("New moon", MoonPhase.phaseName(refNewMoon))
    }

    @Test fun waxing_just_after_new_moon() {
        assertTrue(MoonPhase.isWaxing(refNewMoon + 86_400_000L))
    }
}
