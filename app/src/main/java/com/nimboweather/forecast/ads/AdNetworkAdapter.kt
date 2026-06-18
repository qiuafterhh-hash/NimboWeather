package com.nimboweather.forecast.ads

import android.app.Activity
import android.content.Context

/**
 * One implementation per ad network. AdMob ships first; TopOn / AppLovin MAX /
 * self-built mediation are added later as new adapters with ZERO changes to the
 * business layer (this mirrors the analyzed app's `advert/` abstraction).
 */
interface AdNetworkAdapter {
    val networkName: String

    fun initialize(context: Context, onComplete: () -> Unit = {})

    fun supports(format: AdFormat): Boolean

    /** Preload a full-screen ad (APP_OPEN / INTERSTITIAL). */
    fun preload(context: Context, format: AdFormat, adUnitId: String, callback: AdLoadCallback)

    /** Show a preloaded full-screen ad. Returns false if none was ready. */
    fun showFullScreen(activity: Activity, format: AdFormat, callback: AdShowCallback): Boolean

    fun isReady(format: AdFormat): Boolean
}

interface AdLoadCallback {
    fun onLoaded(format: AdFormat) {}
    fun onFailed(format: AdFormat, error: String) {}
}

interface AdShowCallback {
    fun onShown(format: AdFormat) {}
    fun onDismissed(format: AdFormat) {}
    fun onFailed(format: AdFormat, error: String) {}
}
