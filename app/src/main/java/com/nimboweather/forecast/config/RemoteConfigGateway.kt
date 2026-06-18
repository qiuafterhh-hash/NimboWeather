package com.nimboweather.forecast.config

import android.content.Context
import com.nimboweather.forecast.ads.AdMediator
import com.nimboweather.forecast.prefs.UnitsStore

/**
 * Strategy source with an OFFLINE CACHE (AccuWeather-style: RemoteConfig + local
 * cache). Returns LocalDefault values overlaid with the last cached remote values,
 * so the app always has a usable strategy even offline / before the first fetch.
 *
 * The live Firebase RemoteConfig hookup is pending google-services.json: once wired,
 * the RemoteConfig update listener simply calls [applyRemote] — no other change.
 */
class RemoteConfigGateway(context: Context) : StrategyProvider {

    private val sp = context.getSharedPreferences(UnitsStore.PREFS, Context.MODE_PRIVATE)

    override fun current(): AdStrategy {
        val base = LocalDefaultStrategyProvider().current()
        return base.copy(
            fullScreenNativeMixRatio = sp.getFloat(K_MIX, base.fullScreenNativeMixRatio.toFloat()).toDouble(),
            closeDelaySeconds = sp.getInt(K_DELAY, base.closeDelaySeconds),
            aggressiveProfile = sp.getString(K_PROFILE, base.aggressiveProfile) ?: base.aggressiveProfile
        )
    }

    /** Called by the Firebase RemoteConfig update listener (once wired): caches the
     *  values offline and hot-swaps the live strategy. */
    fun applyRemote(mixRatio: Double, closeDelaySeconds: Int, aggressiveProfile: String) {
        sp.edit()
            .putFloat(K_MIX, mixRatio.toFloat())
            .putInt(K_DELAY, closeDelaySeconds)
            .putString(K_PROFILE, aggressiveProfile)
            .apply()
        AdMediator.updateStrategy(current())
    }

    companion object {
        private const val K_MIX = "rc_native_mix"
        private const val K_DELAY = "rc_close_delay"
        private const val K_PROFILE = "rc_aggressive_profile"
    }
}
