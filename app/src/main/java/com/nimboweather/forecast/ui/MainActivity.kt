package com.nimboweather.forecast.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.nimboweather.forecast.R
import com.nimboweather.forecast.ads.AdFormat
import com.nimboweather.forecast.ads.AdMediator
import com.nimboweather.forecast.ads.BannerLoader
import com.nimboweather.forecast.config.TestAdUnits
import com.nimboweather.forecast.consent.ConsentManager
import com.nimboweather.forecast.databinding.ActivityMainBinding
import com.nimboweather.forecast.location.LocationProvider
import com.nimboweather.forecast.ui.findcity.FindCityActivity
import com.nimboweather.forecast.ui.home.HomeCard
import com.nimboweather.forecast.ui.home.HomeCardRenderer
import com.nimboweather.forecast.ui.home.SkyGradient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val renderer by lazy { HomeCardRenderer(this) }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Light icons on the (dark) sky gradient.
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        // Edge-to-edge (forced on API 35): pad for status bar + nav bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.bannerContainer.updatePadding(bottom = bars.bottom)
            binding.drawerContent.updatePadding(top = bars.top)
            insets
        }

        // Gather UMP consent, then init ad SDKs + load the banner.
        ConsentManager.gather(this) {
            AdMediator.initializeAds(this)
            BannerLoader.attach(binding.bannerContainer, TestAdUnits.BANNER)
        }

        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_units) { viewModel.toggleUnits(); true } else false
        }
        binding.btnAddCity.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, FindCityActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.start() }

        lifecycleScope.launch { viewModel.state.collect { render(it) } }

        val perms = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permLauncher.launch(perms.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
        populateDrawer()
        viewModel.start()
    }

    private fun populateDrawer() {
        val container = binding.drawerCities
        container.removeAllViews()
        viewModel.savedCities().forEach { city ->
            val row = layoutInflater.inflate(R.layout.item_drawer_city, container, false)
            row.findViewById<TextView>(R.id.tvDrawerCity).text = city.display()
            row.setOnClickListener {
                viewModel.selectCity(city)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                // Monetization moment: interstitial on city switch (may render native full-screen).
                AdMediator.maybeShow(this, AdFormat.INTERSTITIAL, placement = "city_switch")
            }
            container.addView(row)
        }
    }

    private fun render(state: MainViewModel.UiState) {
        when (state) {
            is MainViewModel.UiState.Loading -> {
                if (!binding.swipeRefresh.isRefreshing) binding.progress.visibility = View.VISIBLE
            }
            is MainViewModel.UiState.Data -> {
                binding.progress.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                val icon = state.cards.firstNotNullOfOrNull { (it as? HomeCard.Current)?.icon }
                binding.contentRoot.background = SkyGradient.drawable(icon)
                renderer.render(binding.llContent, state.cards)
            }
            is MainViewModel.UiState.Error -> {
                binding.progress.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}
