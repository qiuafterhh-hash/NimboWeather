package com.nimboweather.forecast

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.nimboweather.forecast.ads.AdMediator
import com.nimboweather.forecast.ads.AppOpenAdManager
import com.nimboweather.forecast.ads.adapters.AdmobAdapter
import com.nimboweather.forecast.config.RemoteConfigGateway
import com.nimboweather.forecast.notify.Notifications
import com.nimboweather.forecast.work.WeatherScheduler

class NimboApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ads: register networks behind the abstraction; SDK init waits for UMP consent.
        AdMediator.register(
            adapters = listOf(AdmobAdapter()),
            strategyProvider = RemoteConfigGateway(this) // offline-cached; Firebase live hookup pending google-services.json
        )
        // App-Open ad on cold start / return to foreground.
        val appOpen = AppOpenAdManager()
        registerActivityLifecycleCallbacks(appOpen)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appOpen)

        // Retention: notification channel + periodic background refresh.
        Notifications.ensureChannels(this)
        WeatherScheduler.schedule(this)
    }
}
