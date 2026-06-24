package com.nimboweather.forecast.prefs

import android.content.Context

/** Misc app flags (onboarding done, persistent-notification toggle). */
class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences(UnitsStore.PREFS, Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean(KEY_ONBOARD, false)
        set(v) { sp.edit().putBoolean(KEY_ONBOARD, v).apply() }

    var persistentNotification: Boolean
        get() = sp.getBoolean(KEY_PERSIST, false)
        set(v) { sp.edit().putBoolean(KEY_PERSIST, v).apply() }

    /** Last fired nowcast rain-episode key (null = none pending); dedups push per episode. */
    var nowcastEventKey: String?
        get() = sp.getString(KEY_NOWCAST_EVENT, null)
        set(v) { sp.edit().putString(KEY_NOWCAST_EVENT, v).apply() }

    companion object {
        private const val KEY_ONBOARD = "onboarding_done"
        private const val KEY_PERSIST = "persistent_notif"
        private const val KEY_NOWCAST_EVENT = "nowcast_event_key"
    }
}
