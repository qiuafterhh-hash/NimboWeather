package com.nimboweather.forecast.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nimboweather.forecast.ui.ads.NativeFullscreenActivity

/**
 * Shows an App-Open ad when the app comes to the foreground (cold start + return
 * from background), gated by AdMediator's frequency rules. Never shows over an
 * existing full-screen ad or our native full-screen container.
 */
class AppOpenAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var currentActivity: Activity? = null

    override fun onStart(owner: LifecycleOwner) {
        val act = currentActivity ?: return
        if (AdMediator.isShowingFullScreen()) return
        if (act is NativeFullscreenActivity) return
        AdMediator.maybeShow(act, AdFormat.APP_OPEN, placement = "app_foreground")
    }

    // --- track the visible activity ---
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
