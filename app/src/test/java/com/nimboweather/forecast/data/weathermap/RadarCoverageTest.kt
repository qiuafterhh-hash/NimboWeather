package com.nimboweather.forecast.data.weathermap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarCoverageTest {
    @Test fun us_point_is_covered() {
        // Kansas City, central US
        assertTrue(RadarCoverage.hasNexrad(lat = 39.1, lon = -94.6))
    }

    @Test fun jakarta_is_not_covered() {
        assertFalse(RadarCoverage.hasNexrad(lat = -6.2, lon = 106.8))
    }

    @Test fun london_is_not_covered() {
        assertFalse(RadarCoverage.hasNexrad(lat = 51.5, lon = -0.13))
    }
}
