package com.nimboweather.forecast.widget

import com.nimboweather.forecast.widget.WidgetVisuals.Scene
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetVisualsTest {

    @Test fun clearDayVsNight() {
        assertEquals(Scene.CLEAR_DAY, WidgetVisuals.scene("01d"))
        assertEquals(Scene.CLEAR_NIGHT, WidgetVisuals.scene("01n"))
    }

    @Test fun cloudsDayVsNight() {
        assertEquals(Scene.CLOUDS_DAY, WidgetVisuals.scene("02d"))
        assertEquals(Scene.CLOUDS_NIGHT, WidgetVisuals.scene("03n"))
        assertEquals(Scene.CLOUDS_DAY, WidgetVisuals.scene("04d"))
    }

    @Test fun rainStormSnowMist_dayNightAgnostic() {
        assertEquals(Scene.RAIN, WidgetVisuals.scene("09d"))
        assertEquals(Scene.RAIN, WidgetVisuals.scene("10n"))
        assertEquals(Scene.STORM, WidgetVisuals.scene("11d"))
        assertEquals(Scene.SNOW, WidgetVisuals.scene("13n"))
        assertEquals(Scene.MIST, WidgetVisuals.scene("50d"))
    }

    @Test fun nullOrUnknown_fallsBackToClearDay() {
        assertEquals(Scene.CLEAR_DAY, WidgetVisuals.scene(null))
        assertEquals(Scene.CLEAR_DAY, WidgetVisuals.scene(""))
        assertEquals(Scene.CLEAR_DAY, WidgetVisuals.scene("99x"))
    }
}
