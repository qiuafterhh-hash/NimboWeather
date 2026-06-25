package com.nimboweather.forecast.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FxMapperTest {

    // ---- sceneFrom ----
    @Test fun scene_storm() = assertEquals(FxScene.STORM, FxMapper.sceneFrom("11d"))
    @Test fun scene_rain_shower() = assertEquals(FxScene.RAIN, FxMapper.sceneFrom("09n"))
    @Test fun scene_rain() = assertEquals(FxScene.RAIN, FxMapper.sceneFrom("10d"))
    @Test fun scene_snow() = assertEquals(FxScene.SNOW, FxMapper.sceneFrom("13d"))
    @Test fun scene_fog() = assertEquals(FxScene.FOG, FxMapper.sceneFrom("50n"))
    @Test fun scene_clear_day() = assertEquals(FxScene.CLEAR_DAY, FxMapper.sceneFrom("01d"))
    @Test fun scene_clear_night() = assertEquals(FxScene.CLEAR_NIGHT, FxMapper.sceneFrom("01n"))
    @Test fun scene_fewclouds_night_is_stars() = assertEquals(FxScene.CLEAR_NIGHT, FxMapper.sceneFrom("02n"))
    @Test fun scene_fewclouds_day_is_clouds() = assertEquals(FxScene.CLOUDS, FxMapper.sceneFrom("02d"))
    @Test fun scene_scattered_clouds() = assertEquals(FxScene.CLOUDS, FxMapper.sceneFrom("03n"))
    @Test fun scene_overcast() = assertEquals(FxScene.OVERCAST, FxMapper.sceneFrom("04d"))
    @Test fun scene_null_is_none() = assertEquals(FxScene.NONE, FxMapper.sceneFrom(null))

    // ---- intensityFrom (priority: nowcast peak > rainProb > icon) ----
    @Test fun intensity_uses_nowcast_peak() {
        val i = FxMapper.intensityFrom(listOf(0.0, 6.0, 1.0), rainProb = 10, icon = "10d")
        assertEquals(1.0f, i, 0.05f)
    }
    @Test fun intensity_falls_back_to_rainProb_when_series_empty() {
        val i = FxMapper.intensityFrom(emptyList(), rainProb = 80, icon = "10d")
        assertEquals(0.8f, i, 0.01f)
    }
    @Test fun intensity_falls_back_to_icon_when_no_data() {
        val i = FxMapper.intensityFrom(null, rainProb = null, icon = "11d")
        assertEquals(0.85f, i, 0.01f)
    }

    // ---- tilt: meteorological windDeg = direction wind blows FROM ----
    @Test fun tilt_wind_from_west_drifts_right() {
        assertTrue(FxMapper.tilt(270, 10f) > 0f)   // 西风 → 向东(右)
    }
    @Test fun tilt_wind_from_east_drifts_left() {
        assertTrue(FxMapper.tilt(90, 10f) < 0f)    // 东风 → 向西(左)
    }
    @Test fun tilt_zero_when_no_wind() {
        assertEquals(0f, FxMapper.tilt(270, 0f), 0.001f)
        assertEquals(0f, FxMapper.tilt(null, 10f), 0.001f)
    }
    @Test fun tilt_is_capped() {
        assertTrue(Math.abs(FxMapper.tilt(270, 100f)) <= 0.6f)
    }
}
