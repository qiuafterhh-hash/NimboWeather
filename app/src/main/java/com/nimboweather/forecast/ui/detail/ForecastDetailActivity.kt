package com.nimboweather.forecast.ui.detail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.nimboweather.forecast.databinding.ActivityForecastDetailBinding
import com.nimboweather.forecast.ui.adapter.DailyAdapter
import com.nimboweather.forecast.ui.adapter.HourlyAdapter

/** Full hourly + daily detail, opened by tapping the dial / hourly / daily cards. */
class ForecastDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForecastDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForecastDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            insets
        }

        binding.toolbar.title = DetailHolder.place
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvHourly.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvHourly.adapter = HourlyAdapter().apply { submit(DetailHolder.hourly) }

        binding.rvDaily.layoutManager = LinearLayoutManager(this)
        binding.rvDaily.isNestedScrollingEnabled = false
        binding.rvDaily.adapter = DailyAdapter().apply { submit(DetailHolder.daily) }
    }
}
