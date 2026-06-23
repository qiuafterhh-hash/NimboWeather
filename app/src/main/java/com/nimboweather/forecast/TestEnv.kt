package com.nimboweather.forecast

/**
 * Runtime "automation" switch. When [active], the app renders WeatherFxView as a
 * single static frame instead of an endless redraw loop, so Espresso can reach an
 * idle view hierarchy. Set from instrumented tests (directly) or via the
 * `nimbo_test` launch extra (SplashActivity). Production behaviour is unchanged.
 *
 * Note: the cold-start App-Open ad — the other thing that breaks UI tests (it
 * steals window focus) — is gated separately on [BuildConfig.DEBUG] so it is off
 * in every test build without needing this flag plumbed through each runner.
 */
object TestEnv {

    @Volatile
    var forced = false

    val active: Boolean
        get() = forced
}
