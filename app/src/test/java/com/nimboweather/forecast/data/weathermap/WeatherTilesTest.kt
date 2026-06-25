package com.nimboweather.forecast.data.weathermap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherTilesUrlTest {
    @Test fun owm_url_has_layer_zxy_and_key() {
        val url = WeatherTiles.owmUrl("temp_new", z = 5, x = 3, y = 7, key = "KEY123")
        assertEquals(
            "https://tile.openweathermap.org/map/temp_new/5/3/7.png?appid=KEY123", url
        )
    }

    @Test fun esri_url_uses_z_y_x_order_with_token() {
        val url = WeatherTiles.esriUrl(z = 5, x = 3, y = 7, token = "TOK")
        assertEquals(
            "https://ibasemaps-api.arcgis.com/arcgis/rest/services/World_Topo_Map/" +
                "MapServer/tile/5/7/3?token=TOK", url
        )
    }

    @Test fun nexrad_url_is_zxy() {
        val url = WeatherTiles.nexradUrl(z = 5, x = 3, y = 7)
        assertEquals(
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/" +
                "nexrad-n0q-900913/5/3/7.png", url
        )
    }
}

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
