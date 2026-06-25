package com.nimboweather.forecast.ui.ads

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView
import com.nimboweather.forecast.R
import com.nimboweather.forecast.ads.AdMediator
import com.nimboweather.forecast.ads.NativeAdHolder
import com.nimboweather.forecast.ui.applySystemBarInsets
import com.nimboweather.forecast.ui.applySystemBarMargins

/**
 * Renders a NativeAd full-screen (the "native disguised as full-screen interstitial"
 * play). Close button reveal is delayed by [AdStrategy.closeDelaySeconds] — fully
 * server-configurable. Compliant baseline: close is always reachable (eventually),
 * never fake; back is allowed once closable.
 */
class NativeFullscreenActivity : AppCompatActivity() {

    private var closable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_fullscreen)

        val ad = NativeAdHolder.ad
        if (ad == null) { finish(); return }

        val adView = findViewById<NativeAdView>(R.id.nativeAdView)
        adView.applySystemBarInsets(top = true, bottom = true, left = true, right = true)
        findViewById<View>(R.id.closeOverlay).applySystemBarMargins(top = true, right = true)
        val tvHeadline = findViewById<TextView>(R.id.tvHeadline)
        val tvBody = findViewById<TextView>(R.id.tvBody)
        val ivIcon = findViewById<ImageView>(R.id.ivIcon)
        val media = findViewById<MediaView>(R.id.adMedia)
        val btnCta = findViewById<Button>(R.id.btnCta)

        tvHeadline.text = ad.headline
        adView.headlineView = tvHeadline
        ad.body?.let { tvBody.text = it; tvBody.visibility = View.VISIBLE }
        adView.bodyView = tvBody
        ad.icon?.drawable?.let { ivIcon.setImageDrawable(it); ivIcon.visibility = View.VISIBLE }
        adView.iconView = ivIcon
        ad.mediaContent?.let { media.mediaContent = it }
        adView.mediaView = media
        ad.callToAction?.let { btnCta.text = it; btnCta.visibility = View.VISIBLE }
        adView.callToActionView = btnCta
        adView.setNativeAd(ad)

        val btnClose = findViewById<TextView>(R.id.btnClose)
        val tvCountdown = findViewById<TextView>(R.id.tvCountdown)
        btnClose.setOnClickListener { closeAd() }

        val delay = AdMediator.currentStrategy().closeDelaySeconds
        if (delay <= 0) {
            revealClose(btnClose, tvCountdown)
        } else {
            btnClose.visibility = View.GONE
            tvCountdown.visibility = View.VISIBLE
            object : CountDownTimer(delay * 1000L, 1000) {
                override fun onTick(ms: Long) { tvCountdown.text = ((ms / 1000) + 1).toString() }
                override fun onFinish() { revealClose(btnClose, tvCountdown) }
            }.start()
        }
    }

    private fun revealClose(btnClose: View, tvCountdown: View) {
        closable = true
        tvCountdown.visibility = View.GONE
        btnClose.visibility = View.VISIBLE
    }

    private fun closeAd() {
        NativeAdHolder.onClosed?.invoke()
        NativeAdHolder.clear()
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (closable) closeAd()
        // else ignore until the close button is revealed
    }
}
