package com.nimboweather.forecast.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetAppearanceTest {

    @Test fun defaults_areOpaqueClassic() {
        val a = WidgetAppearance()
        assertEquals(WidgetStyle.CLASSIC, a.widgetStyle)
        assertEquals(255, a.imageAlpha())
        assertEquals(100, a.opacityPercent())
    }

    @Test fun imageAlpha_scalesOpacityTo255() {
        assertEquals(0, WidgetAppearance(bgOpacity = 0f).imageAlpha())
        assertEquals(127, WidgetAppearance(bgOpacity = 0.5f).imageAlpha())
        assertEquals(255, WidgetAppearance(bgOpacity = 1f).imageAlpha())
    }

    @Test fun imageAlpha_clampsOutOfRange() {
        assertEquals(255, WidgetAppearance(bgOpacity = 2f).imageAlpha())
        assertEquals(0, WidgetAppearance(bgOpacity = -1f).imageAlpha())
    }

    @Test fun withOpacity_mapsPercentToFraction() {
        assertEquals(0.4f, WidgetAppearance().withOpacity(40).bgOpacity, 1e-6f)
        assertEquals(102, WidgetAppearance().withOpacity(40).imageAlpha()) // 0.4*255
        // out-of-range percent is clamped
        assertEquals(1f, WidgetAppearance().withOpacity(150).bgOpacity, 1e-6f)
        assertEquals(0f, WidgetAppearance().withOpacity(-5).bgOpacity, 1e-6f)
    }

    @Test fun scrimArgb_alphaTracksOpacity_colorTracksTheme() {
        // fully transparent → alpha 0, regardless of theme color
        assertEquals(0x00000000, WidgetAppearance(bgOpacity = 0f, theme = WidgetAppearance.Theme.DARK).scrimArgb())
        // solid dark = opaque black
        assertEquals(0xFF000000.toInt(), WidgetAppearance(bgOpacity = 1f, theme = WidgetAppearance.Theme.DARK).scrimArgb())
        // solid light = opaque white
        assertEquals(0xFFFFFFFF.toInt(), WidgetAppearance(bgOpacity = 1f, theme = WidgetAppearance.Theme.LIGHT).scrimArgb())
    }

    @Test fun autoTheme_followsSystemDarkFlag() {
        val auto = WidgetAppearance(bgOpacity = 1f, theme = WidgetAppearance.Theme.AUTO)
        assertEquals(0xFF000000.toInt(), auto.scrimArgb(systemDark = true))
        assertEquals(0xFFFFFFFF.toInt(), auto.scrimArgb(systemDark = false))
    }

    @Test fun style_roundTripsThroughKey() {
        assertEquals(WidgetStyle.GLASS, WidgetStyle.fromKey("glass"))
        assertEquals(WidgetStyle.DEFAULT, WidgetStyle.fromKey("nonexistent"))
        assertEquals(WidgetStyle.DEFAULT, WidgetStyle.fromKey(null))
    }
}
