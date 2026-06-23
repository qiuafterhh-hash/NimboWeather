package com.nimboweather.forecast.widget

import kotlinx.serialization.Serializable

/**
 * Per-widget appearance, persisted by `appWidgetId` via [com.nimboweather.forecast.prefs.WidgetPrefs].
 *
 * Mirrors the technique decoded from the competitor (`ForAppWidgetConfig.ClassicWidgetConfig`):
 * a background **opacity** float + an **accent** color, keyed per widget instance. Unlike a fixed
 * drawable background, the opacity scrim lets users get a true transparent widget — a high-intent
 * Play-Store search term.
 *
 * Deliberately framework-free (no `R`, no `android.graphics`) so the color math is unit-testable,
 * matching the style of [WidgetFormat] / [WidgetVisuals]. The provider applies the returned ARGB
 * ints via `RemoteViews.setInt(id, "setBackgroundColor"/"setTextColor", …)`.
 */
@Serializable
data class WidgetAppearance(
    val style: String = WidgetStyle.DEFAULT.key,
    /** 0f = fully transparent, 1f = solid. Stored, not clamped, until [scrimArgb]. */
    val bgOpacity: Float = 1f,
    val theme: Theme = Theme.AUTO,
    /** Accent ARGB (text/glyph tint when the layout opts in). Default = warm amber. */
    val accent: Int = 0xFFFFD08A.toInt(),
) {
    enum class Theme { AUTO, LIGHT, DARK }

    val widgetStyle: WidgetStyle get() = WidgetStyle.fromKey(style)

    /**
     * ARGB for the full-bleed scrim that sits above the scene background and below the content.
     * Alpha tracks [bgOpacity]; base color tracks the resolved light/dark theme.
     *
     * @param systemDark whether the host is in dark mode — only consulted for [Theme.AUTO].
     */
    fun scrimArgb(systemDark: Boolean = true): Int {
        val a = (bgOpacity.coerceIn(0f, 1f) * 255f).toInt()
        val base = if (isDark(systemDark)) 0x000000 else 0xFFFFFF
        return (a shl 24) or base
    }

    /** Primary text color for the resolved theme (opaque). */
    fun textColor(systemDark: Boolean = true): Int =
        if (isDark(systemDark)) 0xFFFFFFFF.toInt() else 0xFF101114.toInt()

    /** Secondary / muted text (hi-lo, "updated" label). */
    fun secondaryTextColor(systemDark: Boolean = true): Int =
        if (isDark(systemDark)) 0x99FFFFFF.toInt() else 0x99101114.toInt()

    private fun isDark(systemDark: Boolean): Boolean = when (theme) {
        Theme.DARK -> true
        Theme.LIGHT -> false
        Theme.AUTO -> systemDark
    }

    fun withOpacity(percent: Int): WidgetAppearance =
        copy(bgOpacity = (percent / 100f).coerceIn(0f, 1f))

    /** 0..255 alpha for the scene-background ImageView (`RemoteViews.setImageAlpha`). */
    fun imageAlpha(): Int = (bgOpacity.coerceIn(0f, 1f) * 255f).toInt()

    /** Opacity as a 0..100 SeekBar position. */
    fun opacityPercent(): Int = (bgOpacity.coerceIn(0f, 1f) * 100f).toInt()
}
