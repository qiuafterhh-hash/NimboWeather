package com.nimboweather.forecast.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NowcastTest {

    // --- evaluate(): the pure state machine ---------------------------------

    @Test fun empty_series_is_dry() {
        assertEquals(NowcastState.Dry, Nowcast.evaluate(emptyList()))
    }

    @Test fun all_zero_is_dry() {
        assertEquals(NowcastState.Dry, Nowcast.evaluate(listOf(0.0, 0.0, 0.0, 0.0)))
    }

    @Test fun all_below_threshold_is_dry() {
        // 0.05 mm trace amounts stay under the 0.1 mm threshold.
        assertEquals(NowcastState.Dry, Nowcast.evaluate(listOf(0.05, 0.09, 0.0, 0.05)))
    }

    @Test fun dry_now_rain_next_step_starts_in_15() {
        assertEquals(
            NowcastState.RainStarting(15),
            Nowcast.evaluate(listOf(0.0, 0.4, 0.4, 0.2))
        )
    }

    @Test fun dry_now_rain_third_step_starts_in_45() {
        assertEquals(
            NowcastState.RainStarting(45),
            Nowcast.evaluate(listOf(0.0, 0.0, 0.0, 0.3))
        )
    }

    @Test fun threshold_value_counts_as_rain() {
        // Exactly 0.1 mm is rain (>= threshold), so this is starting in 15 min.
        assertEquals(
            NowcastState.RainStarting(15),
            Nowcast.evaluate(listOf(0.0, 0.1))
        )
    }

    @Test fun raining_now_then_clears_is_stopping() {
        // Wet for two steps, dry from index 2 → stops in 30 min.
        assertEquals(
            NowcastState.RainStopping(30),
            Nowcast.evaluate(listOf(0.4, 0.2, 0.0, 0.0))
        )
    }

    @Test fun raining_now_clears_immediately_next_step_stops_in_15() {
        assertEquals(
            NowcastState.RainStopping(15),
            Nowcast.evaluate(listOf(0.3, 0.0, 0.0))
        )
    }

    @Test fun raining_throughout_has_no_end() {
        assertEquals(
            NowcastState.RainingThroughout,
            Nowcast.evaluate(listOf(0.4, 0.2, 0.3, 0.5))
        )
    }

    // --- headline(): the copy layer (derives the 5 spec messages) -----------

    @Test fun headline_dry() {
        assertEquals("No rain in the next hour", Nowcast.headline(NowcastState.Dry))
    }

    @Test fun headline_starting_soon_within_15() {
        assertEquals(
            "Rain starting within 15 min",
            Nowcast.headline(NowcastState.RainStarting(15))
        )
    }

    @Test fun headline_starting_later_says_minutes() {
        assertEquals(
            "Rain expected in about 45 min",
            Nowcast.headline(NowcastState.RainStarting(45))
        )
    }

    @Test fun headline_stopping_says_minutes() {
        assertEquals(
            "Rain easing — should stop within 30 min",
            Nowcast.headline(NowcastState.RainStopping(30))
        )
    }

    @Test fun headline_raining_throughout() {
        assertEquals(
            "Rain continuing for the next hour",
            Nowcast.headline(NowcastState.RainingThroughout)
        )
    }
}
