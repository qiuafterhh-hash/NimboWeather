package com.nimboweather.forecast.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimboweather.forecast.data.WeatherRepository
import com.nimboweather.forecast.prefs.UnitsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Loads weather for ONE fixed location (a single pager page). */
class CityWeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WeatherRepository()
    private val builder = WeatherCardsBuilder(app)
    private val unitsStore = UnitsStore(app)

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    fun load(lat: Double, lon: Double, place: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val units = unitsStore.units
                val cur = repo.current(lat, lon, units)
                val fc = repo.forecast(lat, lon, units)
                UiState.Data(builder.build(cur, fc, place))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    sealed interface UiState {
        data object Loading : UiState
        data class Data(val cards: List<HomeCard>) : UiState
        data class Error(val message: String) : UiState
    }
}
