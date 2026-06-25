package com.nimboweather.forecast.data.weathermap.point

import com.nimboweather.forecast.data.weathermap.WeatherLayer
import org.junit.Assert.assertEquals
import org.junit.Test

class PointForecastTest {
    private val resp = OwmPointResponse(
        name = "Jakarta",
        dt = 1_700_000_000L,
        main = OwmMain(temp = 27.4),
        wind = OwmWind(speed = 3.2),
        rain = OwmRain(oneHour = 1.5)
    )

    @Test fun temp_layer_shows_temperature() {
        val p = PointForecast.from(resp, WeatherLayer.TEMP)
        assertEquals("Jakarta", p.place)
        assertEquals("27°C", p.value)
    }

    @Test fun wind_layer_shows_wind_speed() {
        assertEquals("3 m/s", PointForecast.from(resp, WeatherLayer.WIND).value)
    }

    @Test fun precip_layer_shows_rain() {
        assertEquals("1.5 mm", PointForecast.from(resp, WeatherLayer.PRECIP).value)
    }

    @Test fun missing_rain_renders_zero() {
        val dry = resp.copy(rain = null)
        assertEquals("0 mm", PointForecast.from(dry, WeatherLayer.PRECIP).value)
    }

    @Test fun blank_name_falls_back_to_coords_placeholder() {
        val p = PointForecast.from(resp.copy(name = ""), WeatherLayer.TEMP, fallbackPlace = "—")
        assertEquals("—", p.place)
    }

    @Test fun precip_float_artifact_is_rounded_to_one_decimal() {
        val noisy = resp.copy(rain = OwmRain(oneHour = 1.5000000000000002))
        assertEquals("1.5 mm", PointForecast.from(noisy, WeatherLayer.PRECIP).value)
    }
}
