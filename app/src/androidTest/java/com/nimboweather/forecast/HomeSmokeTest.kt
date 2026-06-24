package com.nimboweather.forecast

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.nimboweather.forecast.ui.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke tests — run on the emulator/device (connectedDebugAndroidTest).
 * Launches MainActivity directly (skips splash + onboarding) and verifies the home
 * shell + drawer controls render and respond, independent of network/weather data.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HomeSmokeTest {

    // Pre-grant location so MainActivity's onCreate permission request resolves
    // immediately instead of blocking the UI behind a system dialog.
    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Before
    fun enableTestMode() {
        // Render WeatherFxView statically so the hierarchy reaches idle. (The
        // App-Open ad is already off in debug via BuildConfig.DEBUG.)
        com.nimboweather.forecast.TestEnv.forced = true
    }

    @Test
    fun homeShell_isDisplayed() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
            onView(withId(R.id.btnCityList)).check(matches(isDisplayed()))
            onView(withId(R.id.btnHeatmap)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun drawer_opens_andShowsControls() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.drawerLayout)).perform(DrawerActions.open())
            onView(withId(R.id.btnAddCity)).check(matches(isDisplayed()))
            onView(withId(R.id.switchUnits)).check(matches(isDisplayed()))
            onView(withId(R.id.switchPersistent)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun languageRow_opensDialog() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.drawerLayout)).perform(DrawerActions.open())
            onView(withId(R.id.tvLanguage)).perform(click())
            onView(withText(R.string.language_dialog_title)).check(matches(isDisplayed()))
        }
    }
}
