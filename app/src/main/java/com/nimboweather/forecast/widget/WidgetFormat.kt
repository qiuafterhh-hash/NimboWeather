package com.nimboweather.forecast.widget

/**
 * Pure formatting helpers for the widget's freshness indicator. Kept framework-free so
 * the relative-time / staleness logic is unit-tested.
 */
object WidgetFormat {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /** Threshold past which a snapshot is shown dimmed with a stale dot. */
    private const val STALE_AFTER = 3 * HOUR

    /** "Updated 5m ago" style label. Empty string when never updated. Clock-skew-safe. */
    fun updatedLabel(nowMillis: Long, updatedAt: Long?): String {
        if (updatedAt == null) return ""
        val diff = (nowMillis - updatedAt).coerceAtLeast(0)
        return when {
            diff < MINUTE -> "Updated just now"
            diff < HOUR -> "Updated ${diff / MINUTE}m ago"
            diff < DAY -> "Updated ${diff / HOUR}h ago"
            else -> "Updated ${diff / DAY}d ago"
        }
    }

    fun isStale(nowMillis: Long, updatedAt: Long?): Boolean {
        if (updatedAt == null) return false
        return (nowMillis - updatedAt) > STALE_AFTER
    }
}
