package com.nimboweather.forecast.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AirQualityIndexTest {

    @Test fun pm25_zero_is_zero() {
        assertEquals(0, AirQualityIndex.usAqiFromPm25(0.0))
    }

    @Test fun pm25_band_tops_match_aqi_anchors() {
        assertEquals(50, AirQualityIndex.usAqiFromPm25(12.0))
        assertEquals(100, AirQualityIndex.usAqiFromPm25(35.4))
        assertEquals(150, AirQualityIndex.usAqiFromPm25(55.4))
    }

    @Test fun categories_map_to_ranges() {
        assertEquals("Good", AirQualityIndex.category(40))
        assertEquals("Moderate", AirQualityIndex.category(75))
        assertEquals("Unhealthy", AirQualityIndex.category(180))
        assertEquals("Hazardous", AirQualityIndex.category(350))
    }
}
