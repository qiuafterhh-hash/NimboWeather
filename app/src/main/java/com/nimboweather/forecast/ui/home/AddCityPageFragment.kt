package com.nimboweather.forecast.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.PopularCities
import com.nimboweather.forecast.data.WeatherRepository
import com.nimboweather.forecast.location.LocationProvider
import com.nimboweather.forecast.prefs.SavedCity
import com.nimboweather.forecast.ui.findcity.CityAdapter
import com.nimboweather.forecast.ui.findcity.FindCityViewModel
import kotlinx.coroutines.launch

/** Last pager page: search any city or pick a popular world city to add. */
class AddCityPageFragment : Fragment() {

    private val searchVm: FindCityViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_add_city, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = CityAdapter(R.layout.item_city_light) { geo ->
            (activity as? CityHost)?.addCity(
                SavedCity(name = geo.name ?: "—", country = geo.country, lat = geo.lat, lon = geo.lon)
            )
        }
        val rv = view.findViewById<RecyclerView>(R.id.rvResults)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        adapter.submit(PopularCities.list) // default: popular world cities

        val et = view.findViewById<EditText>(R.id.etQuery)
        view.findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val q = et.text?.toString().orEmpty()
            if (q.isBlank()) adapter.submit(PopularCities.list) else searchVm.search(q)
        }

        view.findViewById<Button>(R.id.btnUseLocation).setOnClickListener {
            val lp = LocationProvider(requireContext())
            if (!lp.hasPermission()) {
                Toast.makeText(requireContext(), R.string.location_permission_needed, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val loc = lp.lastKnown()
                if (loc == null) {
                    Toast.makeText(requireContext(), R.string.location_unavailable, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val (lat, lon) = loc
                val geo = runCatching { WeatherRepository().reverse(lat, lon).firstOrNull() }.getOrNull()
                (activity as? CityHost)?.addCity(
                    SavedCity(name = geo?.name ?: "My location", country = geo?.country, lat = lat, lon = lon)
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            searchVm.results.collect { results ->
                if (results.isNotEmpty()) adapter.submit(results)
            }
        }
    }
}
