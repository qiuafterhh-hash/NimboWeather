package com.nimboweather.forecast.data.nowcast

import com.nimboweather.forecast.data.NowcastState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowcastAlertsTest {

    private val noon = 12

    @Test fun dry_does_not_notify() {
        val r = NowcastAlerts.decide(NowcastState.Dry, lastEventKey = null, hourOfDay = noon)
        assertNull(r.notification)
        assertNull(r.eventKey) // reset, so a later rain event can fire
    }

    @Test fun raining_throughout_does_not_notify() {
        val r = NowcastAlerts.decide(NowcastState.RainingThroughout, lastEventKey = null, hourOfDay = noon)
        assertNull(r.notification)
        assertNull(r.eventKey)
    }

    @Test fun rain_starting_notifies_when_first_seen() {
        val r = NowcastAlerts.decide(NowcastState.RainStarting(15), lastEventKey = null, hourOfDay = noon)
        assertNotNull(r.notification)
        assertEquals("start", r.eventKey)
    }

    @Test fun rain_starting_does_not_renotify_same_event() {
        val r = NowcastAlerts.decide(NowcastState.RainStarting(30), lastEventKey = "start", hourOfDay = noon)
        assertNull(r.notification)
        assertEquals("start", r.eventKey) // key preserved
    }

    @Test fun new_rain_event_after_dry_reset_notifies_again() {
        // Episode ended (lastEventKey cleared by a prior Dry), new rain starts → fire again.
        val r = NowcastAlerts.decide(NowcastState.RainStarting(15), lastEventKey = null, hourOfDay = noon)
        assertNotNull(r.notification)
    }

    @Test fun quiet_hours_suppress_but_preserve_pending_event() {
        // 23:00 is quiet → no notification, and the key is NOT advanced, so it can still
        // fire once quiet hours end and the rain is still imminent.
        val r = NowcastAlerts.decide(NowcastState.RainStarting(15), lastEventKey = null, hourOfDay = 23)
        assertNull(r.notification)
        assertNull(r.eventKey)
    }

    @Test fun quiet_hours_lower_boundary_six_is_quiet() {
        val r = NowcastAlerts.decide(NowcastState.RainStarting(15), lastEventKey = null, hourOfDay = 6)
        assertNull(r.notification)
    }

    @Test fun seven_am_is_not_quiet() {
        val r = NowcastAlerts.decide(NowcastState.RainStarting(15), lastEventKey = null, hourOfDay = 7)
        assertNotNull(r.notification)
    }

    @Test fun ten_pm_is_quiet() {
        val r = NowcastAlerts.decide(NowcastState.RainStarting(15), lastEventKey = null, hourOfDay = 22)
        assertNull(r.notification)
    }

    @Test fun notification_body_mentions_umbrella() {
        val r = NowcastAlerts.decide(NowcastState.RainStarting(30), lastEventKey = null, hourOfDay = noon)
        assertTrue(r.notification!!.body.contains("umbrella", ignoreCase = true))
    }
}
