package com.nimboweather.forecast.prefs

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedCity(
    val name: String,
    val country: String? = null,
    val lat: Double,
    val lon: Double
) {
    fun display(): String = listOfNotNull(name, country).joinToString(", ")
}

/** Persisted saved-cities list + the currently selected city. */
class CityStore(context: Context) {
    private val sp = context.getSharedPreferences(UnitsStore.PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun saved(): List<SavedCity> =
        sp.getString(KEY_LIST, null)?.let {
            runCatching { json.decodeFromString<List<SavedCity>>(it) }.getOrNull()
        } ?: emptyList()

    fun add(city: SavedCity) {
        val list = saved().toMutableList()
        if (list.none { it.lat == city.lat && it.lon == city.lon }) {
            list.add(city)
            saveList(list)
        }
    }

    fun remove(city: SavedCity) {
        saveList(saved().filterNot { it.lat == city.lat && it.lon == city.lon })
    }

    private fun saveList(list: List<SavedCity>) {
        sp.edit().putString(KEY_LIST, json.encodeToString(list)).apply()
    }

    var selected: SavedCity?
        get() = sp.getString(KEY_SELECTED, null)?.let {
            runCatching { json.decodeFromString<SavedCity>(it) }.getOrNull()
        }
        set(value) {
            sp.edit().putString(KEY_SELECTED, value?.let { json.encodeToString(it) }).apply()
        }

    companion object {
        private const val KEY_LIST = "cities"
        private const val KEY_SELECTED = "selected_city"
    }
}
