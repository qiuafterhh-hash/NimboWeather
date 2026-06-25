package com.nimboweather.forecast.data.weathermap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherLayerTest {
    @Test fun temp_precip_wind_carry_owm_layer_ids() {
        assertEquals("temp_new", WeatherLayer.TEMP.owmLayer)
        assertEquals("precipitation_new", WeatherLayer.PRECIP.owmLayer)
        assertEquals("wind_new", WeatherLayer.WIND.owmLayer)
    }

    @Test fun radar_has_no_owm_layer() {
        assertNull(WeatherLayer.RADAR.owmLayer)
    }

    @Test fun color_scale_min_below_max() {
        WeatherLayer.values().forEach {
            assert(it.scaleMin < it.scaleMax) { "${it.name} scale invalid" }
        }
    }
}
