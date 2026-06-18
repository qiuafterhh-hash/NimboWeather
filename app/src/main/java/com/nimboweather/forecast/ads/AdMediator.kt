package com.nimboweather.forecast.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.nimboweather.forecast.config.AdStrategy
import com.nimboweather.forecast.config.StrategyProvider
import kotlin.random.Random

/**
 * Unified ad entry point. The app never calls a network SDK directly. Frequency
 * control, format mixing (incl. native-as-fullscreen) and network selection are
 * all driven by [AdStrategy] (local default now; RemoteConfig / self-built
 * strategy backend later). Adding TopOn/MAX = register another adapter.
 */
object AdMediator {
    private const val TAG = "AdMediator"

    private var adapters: List<AdNetworkAdapter> = emptyList()
    private var strategy: AdStrategy = AdStrategy()
    private val lastShownAt = mutableMapOf<AdFormat, Long>()
    private val shownCount = mutableMapOf<AdFormat, Int>()
    private var adsInitialized = false

    @Volatile
    private var showingFullScreen = false
    fun isShowingFullScreen(): Boolean = showingFullScreen

    /** Step 1: register adapters + strategy (no SDK init yet — wait for consent). */
    fun register(adapters: List<AdNetworkAdapter>, strategyProvider: StrategyProvider) {
        this.adapters = adapters
        this.strategy = strategyProvider.current()
    }

    /** Step 2: after UMP consent — init networks and preload. */
    fun initializeAds(context: Context) {
        if (adsInitialized) return
        adsInitialized = true
        adapters.forEach { adapter ->
            adapter.initialize(context) { Log.d(TAG, "${adapter.networkName} initialized") }
        }
        preload(context, AdFormat.APP_OPEN)
        preload(context, AdFormat.INTERSTITIAL)
        preload(context, AdFormat.NATIVE)
    }

    fun updateStrategy(newStrategy: AdStrategy) { strategy = newStrategy }
    fun currentStrategy(): AdStrategy = strategy

    fun preload(context: Context, format: AdFormat) {
        val unit = strategy.adUnitFor(format) ?: return
        adapterFor(format)?.preload(context, format, unit, object : AdLoadCallback {
            override fun onFailed(format: AdFormat, error: String) { Log.w(TAG, "preload $format failed: $error") }
        })
    }

    /**
     * Frequency-gated full-screen show for an interstitial slot. May render a
     * native ad full-screen instead (per strategy.fullScreenNativeMixRatio).
     * Returns true only if an ad was actually shown.
     */
    fun maybeShow(activity: Activity, slot: AdFormat, placement: String): Boolean {
        if (!adsInitialized) return false
        if (!strategy.isEnabled(slot, placement)) return false

        val cap = strategy.sessionCap(slot)
        if (cap > 0 && (shownCount[slot] ?: 0) >= cap) {
            Log.d(TAG, "$slot gated by session cap ($cap)"); return false
        }
        val now = System.currentTimeMillis()
        if (now - (lastShownAt[slot] ?: 0L) < strategy.cooldownMs(slot)) {
            Log.d(TAG, "$slot gated by cooldown"); return false
        }

        val shown = showCandidate(activity, slot)
        if (shown) {
            lastShownAt[slot] = now
            shownCount[slot] = (shownCount[slot] ?: 0) + 1
        }
        return shown
    }

    private fun showCandidate(activity: Activity, slot: AdFormat): Boolean {
        // For the interstitial slot, optionally render a native ad full-screen.
        if (slot == AdFormat.INTERSTITIAL && strategy.fullScreenNativeMixRatio > 0.0) {
            val nativeAdapter = adapterFor(AdFormat.NATIVE)
            if (nativeAdapter != null && nativeAdapter.isReady(AdFormat.NATIVE) &&
                Random.nextDouble() < strategy.fullScreenNativeMixRatio
            ) {
                val ok = nativeAdapter.showFullScreen(activity, AdFormat.NATIVE, dismissReload(activity, AdFormat.NATIVE))
                if (ok) { showingFullScreen = true; return true }
            }
        }
        val adapter = adapterFor(slot) ?: return false
        if (!adapter.isReady(slot)) { preload(activity, slot); return false }
        return adapter.showFullScreen(activity, slot, dismissReload(activity, slot)).also {
            if (it) showingFullScreen = true
        }
    }

    private fun dismissReload(activity: Activity, format: AdFormat) = object : AdShowCallback {
        override fun onDismissed(format: AdFormat) { showingFullScreen = false; preload(activity, format) }
        override fun onFailed(format: AdFormat, error: String) {
            showingFullScreen = false; Log.w(TAG, "show $format failed: $error"); preload(activity, format)
        }
    }

    private fun adapterFor(format: AdFormat): AdNetworkAdapter? {
        val preferred = strategy.networkFor(format)
        return adapters.firstOrNull { it.supports(format) && (preferred == null || it.networkName == preferred) }
            ?: adapters.firstOrNull { it.supports(format) }
    }
}
