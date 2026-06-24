package com.nimboweather.forecast.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetFormatTest {

    private val now = 1_000_000_000_000L // fixed reference

    @Test fun nullUpdatedAt_isEmpty() {
        assertEquals("", WidgetFormat.updatedLabel(now, null))
    }

    @Test fun underAMinute_isJustNow() {
        assertEquals("Updated just now", WidgetFormat.updatedLabel(now, now - 30_000))
    }

    @Test fun minutesAgo() {
        assertEquals("Updated 5m ago", WidgetFormat.updatedLabel(now, now - 5 * 60_000))
    }

    @Test fun hoursAgo() {
        assertEquals("Updated 3h ago", WidgetFormat.updatedLabel(now, now - 3 * 60 * 60_000L))
    }

    @Test fun daysAgo() {
        assertEquals("Updated 2d ago", WidgetFormat.updatedLabel(now, now - 2 * 24 * 60 * 60_000L))
    }

    @Test fun futureClockSkew_isJustNow() {
        assertEquals("Updated just now", WidgetFormat.updatedLabel(now, now + 10_000))
    }

    @Test fun stale_whenOlderThanThreshold() {
        assertEquals(false, WidgetFormat.isStale(now, now - 30 * 60_000L))
        assertEquals(true, WidgetFormat.isStale(now, now - 5 * 60 * 60_000L))
        assertEquals(false, WidgetFormat.isStale(now, null))
    }
}
