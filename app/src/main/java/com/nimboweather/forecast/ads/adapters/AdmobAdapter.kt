package com.nimboweather.forecast.ads.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.nimboweather.forecast.ads.AdFormat
import com.nimboweather.forecast.ads.AdLoadCallback
import com.nimboweather.forecast.ads.AdNetworkAdapter
import com.nimboweather.forecast.ads.AdShowCallback
import com.nimboweather.forecast.ads.NativeAdHolder
import com.nimboweather.forecast.ui.ads.NativeFullscreenActivity

/**
 * AdMob implementation. Interstitial + App-Open via the SDK; Banner inline
 * (BannerLoader); Native loaded here and rendered full-screen by
 * [NativeFullscreenActivity] (the "native disguised as full-screen" play).
 */
class AdmobAdapter : AdNetworkAdapter {

    override val networkName: String = "AdMob"

    private var interstitial: InterstitialAd? = null
    private var appOpen: AppOpenAd? = null
    private var nativeAd: NativeAd? = null

    override fun initialize(context: Context, onComplete: () -> Unit) {
        MobileAds.initialize(context) { onComplete() }
    }

    override fun supports(format: AdFormat): Boolean = when (format) {
        AdFormat.INTERSTITIAL, AdFormat.APP_OPEN, AdFormat.BANNER, AdFormat.NATIVE -> true
    }

    override fun isReady(format: AdFormat): Boolean = when (format) {
        AdFormat.INTERSTITIAL -> interstitial != null
        AdFormat.APP_OPEN -> appOpen != null
        AdFormat.NATIVE -> nativeAd != null
        AdFormat.BANNER -> false
    }

    override fun preload(context: Context, format: AdFormat, adUnitId: String, callback: AdLoadCallback) {
        val request = AdRequest.Builder().build()
        when (format) {
            AdFormat.INTERSTITIAL -> InterstitialAd.load(
                context, adUnitId, request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; callback.onLoaded(format) }
                    override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null; callback.onFailed(format, error.message) }
                }
            )

            AdFormat.APP_OPEN -> AppOpenAd.load(
                context, adUnitId, request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) { appOpen = ad; callback.onLoaded(format) }
                    override fun onAdFailedToLoad(error: LoadAdError) { appOpen = null; callback.onFailed(format, error.message) }
                }
            )

            AdFormat.NATIVE -> AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad ->
                    nativeAd = ad
                    callback.onLoaded(format)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        nativeAd = null
                        callback.onFailed(format, error.message)
                    }
                })
                .build()
                .loadAd(request)

            AdFormat.BANNER -> { /* inline via BannerLoader */ }
        }
    }

    override fun showFullScreen(activity: Activity, format: AdFormat, callback: AdShowCallback): Boolean {
        return when (format) {
            AdFormat.INTERSTITIAL -> {
                val ad = interstitial ?: return false
                ad.fullScreenContentCallback = fsCallback(format, callback) { interstitial = null }
                ad.show(activity); true
            }
            AdFormat.APP_OPEN -> {
                val ad = appOpen ?: return false
                ad.fullScreenContentCallback = fsCallback(format, callback) { appOpen = null }
                ad.show(activity); true
            }
            AdFormat.NATIVE -> {
                val ad = nativeAd ?: return false
                NativeAdHolder.ad = ad
                NativeAdHolder.onClosed = {
                    nativeAd = null
                    callback.onDismissed(format)
                }
                callback.onShown(format)
                activity.startActivity(Intent(activity, NativeFullscreenActivity::class.java))
                true
            }
            AdFormat.BANNER -> false
        }
    }

    private fun fsCallback(format: AdFormat, callback: AdShowCallback, clear: () -> Unit) =
        object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = callback.onShown(format)
            override fun onAdDismissedFullScreenContent() { clear(); callback.onDismissed(format) }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { clear(); callback.onFailed(format, error.message) }
        }
}
