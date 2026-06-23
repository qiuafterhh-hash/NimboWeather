package com.nimboweather.forecast

/**
 * Runtime "automation" switch. When active, the app suppresses behaviours that
 * make UI test harnesses non-deterministic — the cold-start App-Open ad (steals
 * window focus) and the continuous WeatherFxView redraw loop (keeps the view
 * hierarchy from ever going idle). Production behaviour is unchanged.
 *
 * Detected two ways:
 *  - [espressoPresent]: instrumented (connectedAndroidTest) runs load Espresso
 *    into the app process, so its presence on the classpath flags a test run.
 *  - [forced]: black-box runners (Maestro / `adb am start`) launch the app with
 *    the `nimbo_test` boolean extra, which SplashActivity flips on here.
 */
object TestEnv {

    @Volatile
    var forced = false

    val active: Boolean
        get() = forced || espressoPresent

    private val espressoPresent: Boolean by lazy {
        try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (_: Throwable) {
            false
        }
    }
}
