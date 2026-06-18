package com.nimboweather.forecast.ads

import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Minimal banner helper. Banners/natives render inline so they don't go through
 *  the full-screen preload/show path. */
object BannerLoader {
    fun attach(container: FrameLayout, adUnitId: String) {
        val adView = AdView(container.context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
        }
        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }
}
