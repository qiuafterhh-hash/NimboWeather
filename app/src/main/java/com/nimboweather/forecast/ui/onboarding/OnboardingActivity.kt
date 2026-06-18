package com.nimboweather.forecast.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.nimboweather.forecast.R
import com.nimboweather.forecast.prefs.AppPrefs
import com.nimboweather.forecast.ui.MainActivity

/** First-run value props + location/notification permission priming. */
class OnboardingActivity : AppCompatActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { finishOnboarding() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permLauncher.launch(perms.toTypedArray())
        }
        findViewById<TextView>(R.id.tvSkip).setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        AppPrefs(this).onboardingDone = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
