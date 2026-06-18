package com.nimboweather.forecast.config

import com.nimboweather.forecast.ui.home.HomeCardType

/**
 * Ordered + enabled home cards (mirrors Local Weather's configurable ll_content
 * card list). Local default now; RemoteConfig / self-built strategy backend will
 * drive this later — adding/reordering/hiding cards becomes a server config push.
 */
object CardLayoutConfig {
    fun order(): List<HomeCardType> = listOf(
        HomeCardType.CURRENT,
        HomeCardType.HOURLY,
        HomeCardType.PRECIP,
        HomeCardType.DETAILS,
        HomeCardType.SUNRISE_SUNSET,
        HomeCardType.DAILY
    )
}
