package com.nimboweather.forecast.config

import com.nimboweather.forecast.ads.AdFormat

/**
 * Server-configurable ad strategy. Delivered by RemoteConfig (M3) and, long term,
 * by a self-built strategy backend (mirrors the analyzed app's pl_wf /
 * realtime_waterfall). Every ad-UX knob — including the native-disguised
 * full-screen + delayed-close behaviour — is a field here so it can be tuned
 * per region / cohort, A/B tested, and killed instantly without a release.
 */
data class AdStrategy(
    val enabledFormats: Set<AdFormat> = setOf(
        AdFormat.APP_OPEN, AdFormat.INTERSTITIAL, AdFormat.NATIVE, AdFormat.BANNER
    ),
    val cooldownMsByFormat: Map<AdFormat, Long> = mapOf(
        AdFormat.INTERSTITIAL to 60_000L,
        AdFormat.APP_OPEN to 30_000L
    ),
    val sessionCapByFormat: Map<AdFormat, Int> = mapOf(
        AdFormat.INTERSTITIAL to 20
    ),
    val adUnits: Map<AdFormat, String> = emptyMap(),
    val preferredNetwork: Map<AdFormat, String> = emptyMap(),

    // ---- Server-configurable full-screen UX (native mix + delayed close) ----
    /** Fraction (0..1) of interstitials rendered as full-screen NATIVE. */
    val fullScreenNativeMixRatio: Double = 0.0,
    /** Delayed-close countdown (seconds) before the close button is tappable. */
    val closeDelaySeconds: Int = 0,
    /** "compliant" (default) | "aggressive" — region/cohort gated server-side. */
    val aggressiveProfile: String = "compliant"
) {
    fun isEnabled(format: AdFormat, placement: String): Boolean = enabledFormats.contains(format)
    fun cooldownMs(format: AdFormat): Long = cooldownMsByFormat[format] ?: 0L
    fun sessionCap(format: AdFormat): Int = sessionCapByFormat[format] ?: 0
    fun adUnitFor(format: AdFormat): String? = adUnits[format]
    fun networkFor(format: AdFormat): String? = preferredNetwork[format]
}
