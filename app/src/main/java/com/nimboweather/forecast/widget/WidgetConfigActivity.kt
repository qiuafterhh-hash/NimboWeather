package com.nimboweather.forecast.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.GeoLocation
import com.nimboweather.forecast.data.PopularCities
import com.nimboweather.forecast.prefs.CityStore
import com.nimboweather.forecast.prefs.SavedCity
import com.nimboweather.forecast.prefs.WidgetPrefs
import com.nimboweather.forecast.ui.findcity.CityAdapter

/** Shown when a widget is added: pick which city this widget tracks. */
class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        setContentView(R.layout.activity_widget_config)

        val saved = CityStore(this).saved().map { GeoLocation(it.name, it.lat, it.lon, it.country) }
        val options = saved + PopularCities.list

        val adapter = CityAdapter(R.layout.item_city_light) { geo -> onPick(geo) }
        val rv = findViewById<RecyclerView>(R.id.rvCities)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        adapter.submit(options)
    }

    private fun onPick(geo: GeoLocation) {
        val city = SavedCity(geo.name ?: "—", geo.country, geo.lat, geo.lon)
        WidgetPrefs(this).setCity(widgetId, city)
        WeatherWidgetProvider.enqueueRefresh(this, widgetId)
        WeatherWidgetProvider.render(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
