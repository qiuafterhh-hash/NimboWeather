package com.nimboweather.forecast.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyHiLoTest {

    private fun item(dt: Long, temp: Double) =
        ForecastItem(dt = dt, main = Main(temp = temp))

    @Test fun emptyList_isNull() {
        assertNull(DailyHiLo.inWindow(emptyList(), 0, 86_400))
    }

    @Test fun noItemsInWindow_isNull() {
        val items = listOf(item(100, 10.0), item(200, 20.0))
        assertNull(DailyHiLo.inWindow(items, 1_000, 2_000))
    }

    @Test fun singleItem_hiEqualsLo() {
        val items = listOf(item(500, 17.4))
        val r = DailyHiLo.inWindow(items, 0, 1_000)!!
        assertEquals(17, r.hi)
        assertEquals(17, r.lo)
    }

    @Test fun picksMinAndMax_roundedToInt() {
        val items = listOf(
            item(100, 12.6),   // → 13 (lo after rounding is 12.4→12 below)
            item(200, 25.4),   // → 25 (hi)
            item(300, 12.4),   // → 12 (lo)
            item(400, 19.0)
        )
        val r = DailyHiLo.inWindow(items, 0, 1_000)!!
        assertEquals(25, r.hi)
        assertEquals(12, r.lo)
    }

    @Test fun windowStartInclusive_endExclusive() {
        val items = listOf(
            item(1_000, 5.0),   // at start → included
            item(1_500, 30.0),  // inside → included
            item(2_000, 99.0)   // at end → excluded
        )
        val r = DailyHiLo.inWindow(items, 1_000, 2_000)!!
        assertEquals(30, r.hi)
        assertEquals(5, r.lo)
    }

    @Test fun ignoresItemsWithoutMain() {
        val items = listOf(
            ForecastItem(dt = 100, main = null),
            item(200, 22.0)
        )
        val r = DailyHiLo.inWindow(items, 0, 1_000)!!
        assertEquals(22, r.hi)
        assertEquals(22, r.lo)
    }
}
