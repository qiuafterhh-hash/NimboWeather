package com.nimboweather.forecast.data.nowcast

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoMapperTest {

    @Test fun null_block_yields_empty_series() {
        // Missing minutely_15 → empty → Nowcast treats as Dry (safe degradation).
        assertEquals(emptyList<Double>(), OpenMeteoMapper.precipSeries(OpenMeteoResponse(null)))
    }

    @Test fun nulls_in_array_become_zero() {
        val resp = OpenMeteoResponse(Minutely15(precipitation = listOf(0.4, null, 0.2, null)))
        assertEquals(listOf(0.4, 0.0, 0.2, 0.0), OpenMeteoMapper.precipSeries(resp))
    }

    @Test fun series_is_capped_at_max_steps() {
        val resp = OpenMeteoResponse(Minutely15(precipitation = listOf(0.1, 0.2, 0.3, 0.4, 0.5)))
        assertEquals(listOf(0.1, 0.2, 0.3), OpenMeteoMapper.precipSeries(resp, maxSteps = 3))
    }

    @Test fun shorter_than_max_passes_through() {
        val resp = OpenMeteoResponse(Minutely15(precipitation = listOf(0.0, 0.1)))
        assertEquals(listOf(0.0, 0.1), OpenMeteoMapper.precipSeries(resp, maxSteps = 8))
    }
}
