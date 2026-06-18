package com.nimboweather.forecast.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nimboweather.forecast.prefs.SavedCity

/** Pages = one per saved city, then a final "add city" page. */
class CityPagerAdapter(
    activity: FragmentActivity,
    private val cities: List<SavedCity>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = cities.size + 1

    override fun createFragment(position: Int): Fragment =
        if (position < cities.size) WeatherPageFragment.newInstance(cities[position])
        else AddCityPageFragment()
}

/** Implemented by the host activity so the add page can register a new city. */
interface CityHost {
    fun addCity(city: SavedCity)
}
