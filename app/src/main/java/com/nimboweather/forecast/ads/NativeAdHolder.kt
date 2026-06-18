package com.nimboweather.forecast.ads

import com.google.android.gms.ads.nativead.NativeAd

/** Hands a loaded NativeAd to the full-screen container Activity (can't pass via Intent). */
object NativeAdHolder {
    var ad: NativeAd? = null
    var onClosed: (() -> Unit)? = null

    fun clear() {
        ad?.destroy()
        ad = null
        onClosed = null
    }
}
