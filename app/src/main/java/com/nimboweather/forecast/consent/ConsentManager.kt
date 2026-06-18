package com.nimboweather.forecast.consent

import android.app.Activity
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google UMP (GDPR/CCPA) consent. Gathers consent then calls [onReady] so ads can
 * initialize. Proceeds on failure too (non-personalized ads), so the app is never
 * blocked from monetizing by a consent hiccup.
 */
object ConsentManager {
    fun gather(activity: Activity, onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> onReady() }
            },
            { _ -> onReady() }
        )
    }
}
