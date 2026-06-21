package com.nimboweather.forecast.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nimboweather.forecast.config.CardLayoutConfig
import com.nimboweather.forecast.data.CurrentWeather
import com.nimboweather.forecast.data.DailyForecast
import com.nimboweather.forecast.data.ForecastResponse
import com.nimboweather.forecast.data.HourlyForecast
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.data.WeatherRepository
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.location.LocationProvider
import com.nimboweather.forecast.prefs.CityStore
import com.nimboweather.forecast.prefs.SavedCity
import com.nimboweather.forecast.prefs.UnitsStore
import com.nimboweather.forecast.ui.detail.DetailHolder
import com.nimboweather.forecast.ui.home.HomeCard
import com.nimboweather.forecast.ui.home.HomeCardType
import com.nimboweather.forecast.ui.home.Metric
import com.nimboweather.forecast.ui.home.PrecipPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WeatherRepository()
    private val unitsStore = UnitsStore(app)
    private val cityStore = CityStore(app)
    private val locationProvider = LocationProvider(app)

    private val dateIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayOut = SimpleDateFormat("EEE", Locale.US)

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    fun savedCities(): List<SavedCity> = cityStore.saved()

    fun start() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val (lat, lon, placeHint) = resolveTarget()
            _state.value = load(lat, lon, placeHint)
        }
    }

    fun toggleUnits() { unitsStore.toggle(); start() }

    fun selectCity(city: SavedCity) {
        cityStore.selected = city
        cityStore.add(city)
        start()
    }

    private suspend fun resolveTarget(): Triple<Double, Double, String?> {
        cityStore.selected?.let { return Triple(it.lat, it.lon, it.display()) }
        locationProvider.lastKnown()?.let { (la, lo) ->
            val name = runCatching { repo.reverse(la, lo).firstOrNull()?.display() }.getOrNull()
            return Triple(la, lo, name)
        }
        return Triple(DEFAULT_LAT, DEFAULT_LON, null)
    }

    private suspend fun load(lat: Double, lon: Double, placeHint: String?): UiState {
        return try {
            val units = unitsStore.units
            val cur = repo.current(lat, lon, units)
            val fc = repo.forecast(lat, lon, units)
            UiState.Data(buildCards(cur, fc, placeHint))
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Unknown error")
        }
    }

    private fun buildCards(cur: CurrentWeather, fc: ForecastResponse, placeHint: String?): List<HomeCard> {
        val sym = unitsStore.tempSymbol()
        val place = placeHint ?: cur.name ?: fc.city?.name ?: "—"
        val hourlyAll = mapHourly(fc, Int.MAX_VALUE)
        val daily = mapDaily(fc)
        val precip = mapPrecip(fc)

        // Full lists for the detail screen.
        DetailHolder.place = place
        DetailHolder.hourly = hourlyAll
        DetailHolder.daily = daily

        val today = daily.firstOrNull()
        val temp = cur.main?.temp?.roundToInt() ?: 0

        // Cache snapshot for the widget + notification.
        WeatherCache(getApplication()).save(
            WeatherSnapshot(
                city = place,
                temp = temp,
                symbol = sym,
                condition = cur.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                icon = cur.weather.firstOrNull()?.icon
            )
        )

        return CardLayoutConfig.order().mapNotNull { type ->
            when (type) {
                HomeCardType.CURRENT -> HomeCard.Current(
                    place = place,
                    temp = temp,
                    symbol = sym,
                    desc = cur.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                    icon = cur.weather.firstOrNull()?.icon,
                    feels = cur.main?.feelsLike?.roundToInt() ?: temp,
                    max = today?.max ?: temp,
                    min = today?.min ?: temp,
                    rainProb = precip.firstOrNull()?.pop ?: 0,
                    pressure = cur.main?.pressure ?: 0,
                    windText = "${degToCompass(cur.wind?.deg)} · ${(cur.wind?.speed ?: 0.0).roundToInt()} ${unitsStore.speedSymbol()}",
                    windDeg = cur.wind?.deg
                )
                HomeCardType.HOURLY -> hourlyAll.take(8).takeIf { it.isNotEmpty() }?.let { HomeCard.Hourly(it) }
                HomeCardType.PRECIP -> precip.takeIf { it.isNotEmpty() }?.let { HomeCard.Precip(it) }
                HomeCardType.DETAILS -> HomeCard.Details(buildMetrics(cur, sym))
                HomeCardType.SUNRISE_SUNSET -> cur.sys?.takeIf { it.sunrise > 0 }?.let {
                    HomeCard.SunriseSunset(fmtTime(it.sunrise, cur.timezone), fmtTime(it.sunset, cur.timezone))
                }
                HomeCardType.DAILY -> daily.takeIf { it.isNotEmpty() }?.let { HomeCard.Daily(it) }
            }
        }
    }

    private fun degToCompass(deg: Int?): String {
        if (deg == null) return "--"
        val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val idx = ((deg / 22.5).roundToInt() % 16 + 16) % 16
        return dirs[idx]
    }

    private fun buildMetrics(cur: CurrentWeather, sym: String): List<Metric> {
        val list = mutableListOf<Metric>()
        cur.main?.feelsLike?.let { list.add(Metric("Feels like", "${it.roundToInt()}$sym")) }
        cur.main?.humidity?.let { list.add(Metric("Humidity", "$it%")) }
        cur.main?.pressure?.let { list.add(Metric("Pressure", "$it hPa")) }
        cur.wind?.speed?.let { list.add(Metric("Wind", "${it.roundToInt()} ${unitsStore.speedSymbol()}")) }
        cur.visibility?.let { list.add(Metric("Visibility", "${it / 1000} km")) }
        cur.clouds?.all?.let { list.add(Metric("Cloudiness", "$it%")) }
        return list
    }

    private fun mapPrecip(fc: ForecastResponse): List<PrecipPoint> =
        fc.list.take(6).map {
            PrecipPoint(
                time = it.dtTxt?.let { t -> if (t.length >= 16) t.substring(11, 16) else t } ?: "--",
                pop = (it.pop * 100).roundToInt()
            )
        }

    private fun mapHourly(fc: ForecastResponse, limit: Int): List<HourlyForecast> =
        fc.list.take(limit).map { item ->
            HourlyForecast(
                timeLabel = item.dtTxt?.let { if (it.length >= 16) it.substring(11, 16) else it } ?: "--",
                temp = item.main?.temp?.roundToInt() ?: 0,
                icon = item.weather.firstOrNull()?.icon
            )
        }

    private fun mapDaily(fc: ForecastResponse): List<DailyForecast> {
        val byDate = fc.list
            .groupBy { it.dtTxt?.substringBefore(' ') ?: "" }
            .filterKeys { it.isNotEmpty() }
        return byDate.entries.take(6).map { (date, items) ->
            val temps = items.mapNotNull { it.main?.temp }
            val midday = items.minByOrNull { abs((it.dtTxt?.substring(11, 13)?.toIntOrNull() ?: 0) - 12) } ?: items.first()
            DailyForecast(
                dayLabel = runCatching { dayOut.format(dateIn.parse(date)!!) }.getOrDefault(date),
                min = (temps.minOrNull() ?: 0.0).roundToInt(),
                max = (temps.maxOrNull() ?: 0.0).roundToInt(),
                icon = midday.weather.firstOrNull()?.icon,
                desc = midday.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() }
            )
        }
    }

    private fun fmtTime(unixUtc: Long, tzOffsetSec: Int): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date((unixUtc + tzOffsetSec) * 1000L))
    }

    sealed interface UiState {
        data object Loading : UiState
        data class Data(val cards: List<HomeCard>) : UiState
        data class Error(val message: String) : UiState
    }

    companion object {
        const val DEFAULT_LAT = 51.5074
        const val DEFAULT_LON = -0.1278
    }
}
