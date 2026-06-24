package com.nimboweather.forecast.widget

import androidx.annotation.DrawableRes
import com.nimboweather.forecast.R

/**
 * Maps an OWM weather icon code (e.g. "10n") to a widget scene, then to the matching
 * background gradient + condition glyph. The scene mapping mirrors ui/home/SkyGradient
 * so the widget stays visually consistent with the app. Scene resolution is pure and
 * unit-tested; the res lookups are thin and depend only on R.
 */
object WidgetVisuals {

    enum class Scene { CLEAR_DAY, CLEAR_NIGHT, CLOUDS_DAY, CLOUDS_NIGHT, RAIN, STORM, SNOW, MIST }

    fun scene(icon: String?): Scene {
        val night = icon?.endsWith("n") == true
        return when (icon?.take(2)) {
            "01" -> if (night) Scene.CLEAR_NIGHT else Scene.CLEAR_DAY
            "02", "03", "04" -> if (night) Scene.CLOUDS_NIGHT else Scene.CLOUDS_DAY
            "09", "10" -> Scene.RAIN
            "11" -> Scene.STORM
            "13" -> Scene.SNOW
            "50" -> Scene.MIST
            else -> Scene.CLEAR_DAY
        }
    }

    @DrawableRes
    fun background(icon: String?): Int = when (scene(icon)) {
        Scene.CLEAR_DAY -> R.drawable.widget_bg_clear_day
        Scene.CLEAR_NIGHT -> R.drawable.widget_bg_clear_night
        Scene.CLOUDS_DAY -> R.drawable.widget_bg_clouds_day
        Scene.CLOUDS_NIGHT -> R.drawable.widget_bg_clouds_night
        Scene.RAIN -> R.drawable.widget_bg_rain
        Scene.STORM -> R.drawable.widget_bg_storm
        Scene.SNOW -> R.drawable.widget_bg_snow
        Scene.MIST -> R.drawable.widget_bg_mist
    }

    @DrawableRes
    fun glyph(icon: String?): Int = when (scene(icon)) {
        Scene.CLEAR_DAY -> R.drawable.ic_wx_sun
        Scene.CLEAR_NIGHT -> R.drawable.ic_moon
        Scene.CLOUDS_DAY -> R.drawable.ic_wx_partly_day
        Scene.CLOUDS_NIGHT -> R.drawable.ic_cloud
        Scene.RAIN -> R.drawable.ic_wx_rain
        Scene.STORM -> R.drawable.ic_wx_storm
        Scene.SNOW -> R.drawable.ic_wx_snow
        Scene.MIST -> R.drawable.ic_wx_fog
    }
}
