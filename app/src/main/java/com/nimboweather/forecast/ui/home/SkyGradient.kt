package com.nimboweather.forecast.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/**
 * Dynamic sky background driven by the OWM weather icon code + day/night
 * (icon suffix d/n). Mirrors the market pattern (Gradient Weather / iOS Weather):
 * colour adapts to conditions, which doubles as the product's primary colour.
 */
object SkyGradient {

    private fun c(hex: String) = Color.parseColor(hex)

    private val clearDay = intArrayOf(c("#1E63E0"), c("#3F86EE"), c("#7FB6FF"))
    private val clearNight = intArrayOf(c("#0A1B38"), c("#142F55"), c("#274A73"))
    private val cloudsDay = intArrayOf(c("#3E6BB8"), c("#5E8AC9"), c("#90B4DE"))
    private val cloudsNight = intArrayOf(c("#14233D"), c("#22364F"), c("#33485F"))
    private val rain = intArrayOf(c("#33435C"), c("#4C667F"), c("#728CA6"))
    private val storm = intArrayOf(c("#22242F"), c("#363B4C"), c("#4E5468"))
    private val snow = intArrayOf(c("#577791"), c("#88A5BC"), c("#BFD2E2"))
    private val mist = intArrayOf(c("#5F6B79"), c("#828D99"), c("#A7AFB9"))

    fun colorsFor(icon: String?): IntArray {
        val night = icon?.endsWith("n") == true
        return when (icon?.take(2)) {
            "01" -> if (night) clearNight else clearDay
            "02", "03", "04" -> if (night) cloudsNight else cloudsDay
            "09", "10" -> rain
            "11" -> storm
            "13" -> snow
            "50" -> mist
            else -> if (night) clearNight else clearDay
        }
    }

    fun drawable(icon: String?): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colorsFor(icon))
}
