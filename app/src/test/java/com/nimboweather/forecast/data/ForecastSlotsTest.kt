package com.nimboweather.forecast.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastSlotsTest {

    private fun item(dt: Long, temp: Double, icon: String = "01d") =
        ForecastItem(dt = dt, main = Main(temp = temp), weather = listOf(WeatherDesc(icon = icon)))

    @Test fun empty_isEmpty() {
        assertEquals(emptyList<ForecastItem>(), ForecastSlots.nextSlots(emptyList(), 0, 3))
    }

    @Test fun dropsPastItems() {
        val items = listOf(item(100, 1.0), item(200, 2.0), item(300, 3.0))
        val r = ForecastSlots.nextSlots(items, 250, 3)
        assertEquals(listOf(300L), r.map { it.dt })
    }

    @Test fun takesOnlyCount() {
        val items = (1..6).map { item(it * 100L, it.toDouble()) }
        val r = ForecastSlots.nextSlots(items, 0, 3)
        assertEquals(listOf(100L, 200L, 300L), r.map { it.dt })
    }

    @Test fun fewerThanCount_returnsAvailable() {
        val items = listOf(item(100, 1.0), item(200, 2.0))
        val r = ForecastSlots.nextSlots(items, 0, 3)
        assertEquals(2, r.size)
    }

    @Test fun nowInclusive_atBoundary() {
        val items = listOf(item(200, 2.0))
        assertEquals(1, ForecastSlots.nextSlots(items, 200, 3).size)
    }

    @Test fun sortsByTime() {
        val items = listOf(item(300, 3.0), item(100, 1.0), item(200, 2.0))
        val r = ForecastSlots.nextSlots(items, 0, 3)
        assertEquals(listOf(100L, 200L, 300L), r.map { it.dt })
    }
}
