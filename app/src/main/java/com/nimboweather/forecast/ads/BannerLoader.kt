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
            setAdSize(adaptiveAdSize(container))
            this.adUnitId = adUnitId
        }
        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    /** Full-width anchored adaptive banner so the ad fills the screen width
     *  instead of the fixed 320dp BANNER (which left gaps on both sides). */
    private fun adaptiveAdSize(container: FrameLayout): AdSize {
        val metrics = container.resources.displayMetrics
        val widthPx = if (container.width > 0) container.width.toFloat() else metrics.widthPixels.toFloat()
        val adWidthDp = (widthPx / metrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(container.context, adWidthDp)
    }
}
