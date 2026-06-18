package com.nimboweather.forecast.ui.findcity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nimboweather.forecast.databinding.ActivityFindCityBinding
import com.nimboweather.forecast.prefs.CityStore
import com.nimboweather.forecast.prefs.SavedCity
import kotlinx.coroutines.launch

class FindCityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindCityBinding
    private val viewModel: FindCityViewModel by viewModels()
    private lateinit var cityStore: CityStore

    private val adapter = CityAdapter { geo ->
        cityStore.selected = SavedCity(
            name = geo.name ?: "—",
            country = geo.country,
            lat = geo.lat,
            lon = geo.lon
        ).also { cityStore.add(it) }
        finish() // MainActivity.onResume reloads for the selected city
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindCityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cityStore = CityStore(this)

        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter

        binding.btnSearch.setOnClickListener {
            viewModel.search(binding.etQuery.text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            viewModel.results.collect { adapter.submit(it) }
        }
        lifecycleScope.launch {
            viewModel.error.collect { msg ->
                if (!msg.isNullOrBlank()) Toast.makeText(this@FindCityActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
