package com.nimboweather.forecast.ui.findcity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimboweather.forecast.data.GeoLocation
import com.nimboweather.forecast.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FindCityViewModel(
    private val repo: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _results = MutableStateFlow<List<GeoLocation>>(emptyList())
    val results: StateFlow<List<GeoLocation>> = _results

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _error.value = null
            _results.value = try {
                repo.geocode(query.trim())
            } catch (e: Exception) {
                _error.value = e.message
                emptyList()
            }
        }
    }
}
