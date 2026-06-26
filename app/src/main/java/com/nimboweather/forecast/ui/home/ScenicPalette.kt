package com.nimboweather.forecast.ui.home

/** Color/flag set for one scene's bottom scenic band. Colors are ARGB ints. */
data class ScenicPalette(
    val farHill: Int,
    val nearHill: Int,
    val accent: Int,
    val snowCaps: Boolean,
    val fogVeil: Boolean
)

/**
 * Pure mapping OWM icon code -> scenic palette. Framework-free (no Android View/Color),
 * unit-tested like FxMapper / AirQualityIndex. Follows SkyGradient's 8-scene taxonomy
 * (02/03/04 = clouds day/night), NOT FxScene's CLOUDS/OVERCAST/clear-night split.
 */
object ScenicPalettes {

    private val clearDay = ScenicPalette(
        farHill = 0xFF2E63B4.toInt(), nearHill = 0xFF1E4684.toInt(),
        accent = 0xFF1C5E3A.toInt(), snowCaps = false, fogVeil = false
    )
    private val clearNight = ScenicPalette(
        farHill = 0xFF1A2E50.toInt(), nearHill = 0xFF101F38.toInt(),
        accent = 0xFF14304A.toInt(), snowCaps = false, fogVeil = false
    )
    private val cloudsDay = ScenicPalette(
        farHill = 0xFF45628C.toInt(), nearHill = 0xFF324862.toInt(),
        accent = 0xFF2A4A38.toInt(), snowCaps = false, fogVeil = false
    )
    private val cloudsNight = ScenicPalette(
        farHill = 0xFF263647.toInt(), nearHill = 0xFF18242F.toInt(),
        accent = 0xFF1E3328.toInt(), snowCaps = false, fogVeil = false
    )
    private val rain = ScenicPalette(
        farHill = 0xFF3A4C5C.toInt(), nearHill = 0xFF28363F.toInt(),
        accent = 0xFF24382E.toInt(), snowCaps = false, fogVeil = false
    )
    private val storm = ScenicPalette(
        farHill = 0xFF2A2D38.toInt(), nearHill = 0xFF1B1D24.toInt(),
        accent = 0xFF1A241D.toInt(), snowCaps = false, fogVeil = false
    )
    private val snow = ScenicPalette(
        farHill = 0xFF6F8190.toInt(), nearHill = 0xFF53606B.toInt(),
        accent = 0xFF2C4438.toInt(), snowCaps = true, fogVeil = false
    )
    private val mist = ScenicPalette(
        farHill = 0xFF6B7682.toInt(), nearHill = 0xFF515A63.toInt(),
        accent = 0xFF3A4A40.toInt(), snowCaps = false, fogVeil = true
    )

    fun from(icon: String?): ScenicPalette {
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
}
