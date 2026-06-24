package com.nimboweather.forecast.widget

/**
 * Visual style families for home-screen widgets. Orthogonal to size (which
 * [WeatherWidgetProvider.chooseLayout] still resolves from the measured cell).
 *
 * Each style maps to a layout *set* — the provider picks small/medium/large within the set.
 * Kept as a plain enum (no R references) so it stays unit-testable and so the layout lookup
 * lives in one place. Add a new style by registering a thin `StyledWidgetProvider` subclass
 * + an `appwidget-provider` XML with a distinct `previewLayout` (see docs/widget-matrix-spec.md).
 */
enum class WidgetStyle(val key: String, val displayName: String) {
    /** Default look that ships today: scene gradient + temp + hi/lo + hourly strip. */
    CLASSIC("classic", "Classic"),

    /** Transparent / frosted variant — background opacity driven by WidgetAppearance. */
    GLASS("glass", "Glass"),

    /** Multi-day forecast row. */
    DAILY("daily", "Daily"),

    /** Hourly chart strip. */
    HOURLY("hourly", "Hourly"),

    /** Clock-forward skin (TextClock + per-city timezone). */
    CLOCK("clock", "Clock");

    companion object {
        val DEFAULT = CLASSIC
        fun fromKey(key: String?): WidgetStyle =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
