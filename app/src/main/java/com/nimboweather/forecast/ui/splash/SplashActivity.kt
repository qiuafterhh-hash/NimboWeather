package com.nimboweather.forecast.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nimboweather.forecast.R
import com.nimboweather.forecast.TestEnv
import com.nimboweather.forecast.prefs.AppPrefs
import com.nimboweather.forecast.ui.MainActivity
import com.nimboweather.forecast.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra("nimbo_test", false) == true) TestEnv.forced = true
        setContentView(R.layout.activity_splash)
        window.decorView.postDelayed({
            val next = if (AppPrefs(this).onboardingDone) MainActivity::class.java else OnboardingActivity::class.java
            startActivity(Intent(this, next))
            finish()
        }, 1200)
    }
}
