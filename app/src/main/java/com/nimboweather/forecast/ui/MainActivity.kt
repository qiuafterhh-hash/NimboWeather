package com.nimboweather.forecast.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.nimboweather.forecast.R
import com.nimboweather.forecast.ads.AdFormat
import com.nimboweather.forecast.ads.AdMediator
import com.nimboweather.forecast.ads.BannerLoader
import com.nimboweather.forecast.config.TestAdUnits
import com.nimboweather.forecast.consent.ConsentManager
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.databinding.ActivityMainBinding
import com.nimboweather.forecast.notify.Notifications
import com.nimboweather.forecast.prefs.AppPrefs
import com.nimboweather.forecast.prefs.CityStore
import com.nimboweather.forecast.prefs.SavedCity
import com.nimboweather.forecast.prefs.UnitsStore
import com.nimboweather.forecast.ui.home.CityHost
import com.nimboweather.forecast.ui.home.CityPagerAdapter

class MainActivity : AppCompatActivity(), CityHost {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cityStore: CityStore
    private lateinit var unitsStore: UnitsStore
    private var cities: List<SavedCity> = emptyList()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { rebuildPager(keepCurrent = true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cityStore = CityStore(this)
        unitsStore = UnitsStore(this)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = bars.top)
            binding.bannerContainer.updatePadding(bottom = bars.bottom)
            binding.drawerContent.updatePadding(top = bars.top)
            insets
        }

        ConsentManager.gather(this) {
            AdMediator.initializeAds(this)
            BannerLoader.attach(binding.bannerContainer, TestAdUnits.BANNER)
        }

        binding.btnCityList.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.btnHeatmap.setOnClickListener {
            startActivity(android.content.Intent(this, com.nimboweather.forecast.ui.radar.RadarActivity::class.java))
        }
        binding.btnAddCity.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.viewPager.setCurrentItem(cities.size, true) // last = add page
        }

        val appPrefs = AppPrefs(this)
        binding.switchPersistent.isChecked = appPrefs.persistentNotification
        binding.switchPersistent.setOnCheckedChangeListener { _, checked ->
            appPrefs.persistentNotification = checked
            Notifications.updatePersistent(this, WeatherCache(this).load(), checked)
        }
        binding.switchUnits.isChecked = !unitsStore.isMetric()
        binding.switchUnits.setOnCheckedChangeListener { _, _ ->
            unitsStore.toggle(); rebuildPager(keepCurrent = true)
        }
        binding.tvLanguage.setOnClickListener { showLanguageDialog() }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateTitle(position)
        })

        // Skip the runtime permission prompt under UI-test automation — perms are
        // pre-granted there, and launching the request can momentarily steal window
        // focus and flake Espresso/Maestro.
        if (!com.nimboweather.forecast.TestEnv.active) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permLauncher.launch(perms.toTypedArray())
        }

        rebuildPager(keepCurrent = false)
    }

    override fun onResume() {
        super.onResume()
        populateDrawer()
    }

    override fun addCity(city: SavedCity) {
        cityStore.add(city)
        cityStore.selected = city
        cities = currentCities()
        binding.viewPager.adapter = CityPagerAdapter(this, cities)
        val idx = cities.indexOfFirst { it.lat == city.lat && it.lon == city.lon }.coerceAtLeast(0)
        binding.viewPager.setCurrentItem(idx, false)
        updateTitle(idx)
        populateDrawer()
        // Monetization moment: interstitial on adding/switching city.
        AdMediator.maybeShow(this, AdFormat.INTERSTITIAL, placement = "city_added")
    }

    private fun rebuildPager(keepCurrent: Boolean) {
        val prev = binding.viewPager.currentItem
        cities = currentCities()
        binding.viewPager.adapter = CityPagerAdapter(this, cities)
        val target = if (keepCurrent) prev.coerceIn(0, cities.size) else selectedIndex()
        binding.viewPager.setCurrentItem(target, false)
        updateTitle(target)
        populateDrawer()
    }

    private fun currentCities(): List<SavedCity> {
        val saved = cityStore.saved()
        return saved.ifEmpty { listOf(DEFAULT_CITY) }
    }

    private fun selectedIndex(): Int {
        val sel = cityStore.selected ?: return 0
        val i = cities.indexOfFirst { it.lat == sel.lat && it.lon == sel.lon }
        return if (i >= 0) i else 0
    }

    private fun updateTitle(position: Int) {
        if (position < cities.size) {
            binding.tvCity.text = cities[position].display()
            cityStore.selected = cities[position]
        } else {
            binding.tvCity.text = getString(R.string.add_city_title)
        }
        binding.pageDots.set(cities.size, position.coerceIn(0, (cities.size - 1).coerceAtLeast(0)))
    }

    private fun populateDrawer() {
        val container = binding.drawerCities
        container.removeAllViews()
        cities.forEachIndexed { idx, city ->
            val row = layoutInflater.inflate(R.layout.item_drawer_city, container, false)
            row.findViewById<TextView>(R.id.tvDrawerCity).text = city.display()
            row.setOnClickListener {
                binding.viewPager.setCurrentItem(idx, false)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            container.addView(row)
        }
    }

    private fun showLanguageDialog() {
        val names = arrayOf(
            getString(R.string.language_system),
            getString(R.string.language_english),
            getString(R.string.language_chinese)
        )
        val tags = arrayOf("", "en", "zh")
        AlertDialog.Builder(this)
            .setTitle(R.string.language_dialog_title)
            .setItems(names) { _, which ->
                val tag = tags[which]
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                    else LocaleListCompat.forLanguageTags(tag)
                )
            }
            .show()
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    companion object {
        private val DEFAULT_CITY = SavedCity("London", "GB", 51.5074, -0.1278)
    }
}
