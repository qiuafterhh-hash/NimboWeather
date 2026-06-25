package com.nimboweather.forecast.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenicPaletteTest {

    @Test fun snow_has_snow_caps() {
        val p = ScenicPalettes.from("13d")
        assertTrue(p.snowCaps)
        assertFalse(p.fogVeil)
    }

    @Test fun fog_has_veil() {
        val p = ScenicPalettes.from("50n")
        assertTrue(p.fogVeil)
        assertFalse(p.snowCaps)
    }

    @Test fun clear_day_has_no_caps_or_veil() {
        val p = ScenicPalettes.from("01d")
        assertFalse(p.snowCaps)
        assertFalse(p.fogVeil)
    }

    @Test fun clear_day_and_night_differ() {
        assertTrue(ScenicPalettes.from("01d") != ScenicPalettes.from("01n"))
    }

    @Test fun clouds_day_and_night_differ() {
        assertTrue(ScenicPalettes.from("04d") != ScenicPalettes.from("04n"))
    }

    @Test fun fewclouds_night_is_clouds_night_not_clear() {
        assertEquals(ScenicPalettes.from("03n"), ScenicPalettes.from("04n"))
        assertTrue(ScenicPalettes.from("02n") != ScenicPalettes.from("01n"))
    }

    @Test fun rain_and_storm_differ() {
        assertTrue(ScenicPalettes.from("10d") != ScenicPalettes.from("11d"))
    }

    @Test fun null_icon_falls_back_to_clear_day() {
        assertEquals(ScenicPalettes.from("01d"), ScenicPalettes.from(null))
    }
}
