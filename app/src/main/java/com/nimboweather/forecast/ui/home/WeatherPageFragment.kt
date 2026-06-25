package com.nimboweather.forecast.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nimboweather.forecast.R
import com.nimboweather.forecast.prefs.SavedCity
import kotlinx.coroutines.launch

/** One swipeable page = one city's weather (dial hero + card stream). */
class WeatherPageFragment : Fragment() {

    private val vm: CityWeatherViewModel by viewModels()
    private val renderer by lazy { HomeCardRenderer(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_weather_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val lat = requireArguments().getDouble(ARG_LAT)
        val lon = requireArguments().getDouble(ARG_LON)
        val place = requireArguments().getString(ARG_PLACE) ?: "—"

        val root = view.findViewById<View>(R.id.pageRoot)
        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val content = view.findViewById<LinearLayout>(R.id.llContent)

        swipe.setOnRefreshListener { vm.load(lat, lon, place) }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                when (state) {
                    is CityWeatherViewModel.UiState.Loading -> swipe.isRefreshing = true
                    is CityWeatherViewModel.UiState.Data -> {
                        swipe.isRefreshing = false
                        val current = state.cards.firstNotNullOfOrNull { it as? HomeCard.Current }
                        val icon = current?.icon
                        val nowcastSeries = state.cards.firstNotNullOfOrNull { (it as? HomeCard.Nowcast)?.series }
                        root.background = SkyGradient.drawable(icon)
                        view.findViewById<WeatherScenicView>(R.id.weatherScenic).setScene(icon)
                        view.findViewById<WeatherFxView>(R.id.weatherFx).setSpec(
                            FxSpec(
                                scene = FxMapper.sceneFrom(icon),
                                windDeg = current?.windDeg,
                                windSpeed = current?.windSpeed ?: 0f,
                                intensity = FxMapper.intensityFrom(nowcastSeries, current?.rainProb, icon)
                            )
                        )
                        renderer.render(content, state.cards)
                    }
                    is CityWeatherViewModel.UiState.Error -> swipe.isRefreshing = false
                }
            }
        }
        vm.load(lat, lon, place)
    }

    companion object {
        private const val ARG_LAT = "lat"
        private const val ARG_LON = "lon"
        private const val ARG_PLACE = "place"

        fun newInstance(city: SavedCity): WeatherPageFragment = WeatherPageFragment().apply {
            arguments = Bundle().apply {
                putDouble(ARG_LAT, city.lat)
                putDouble(ARG_LON, city.lon)
                putString(ARG_PLACE, city.display())
            }
        }
    }
}
