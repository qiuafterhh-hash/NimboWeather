package com.nimboweather.forecast.config

import com.nimboweather.forecast.ads.AdFormat

/** Source of [AdStrategy]. LocalDefault now; FirebaseRemoteConfigProvider in M3. */
interface StrategyProvider {
    fun current(): AdStrategy
}

/** Compliant defaults, AdMob official TEST ad units (safe to ship in M0). */
class LocalDefaultStrategyProvider : StrategyProvider {
    override fun current(): AdStrategy = AdStrategy(
        adUnits = mapOf(
            AdFormat.APP_OPEN to TestAdUnits.APP_OPEN,
            AdFormat.INTERSTITIAL to TestAdUnits.INTERSTITIAL,
            AdFormat.NATIVE to TestAdUnits.NATIVE,
            AdFormat.BANNER to TestAdUnits.BANNER
        ),
        // Dev defaults so the native-fullscreen path is testable. Server (RemoteConfig)
        // overrides these per region/cohort; compliant baseline keeps close immediate.
        fullScreenNativeMixRatio = 0.5,
        closeDelaySeconds = 2,
        aggressiveProfile = "compliant"
    )
}

/** Google's official test ad unit IDs (no real fill, no policy risk in dev). */
object TestAdUnits {
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
}
