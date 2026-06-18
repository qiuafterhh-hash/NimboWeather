package com.nimboweather.forecast.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.ui.MainActivity

/** Home-screen widget showing the cached current weather; tap opens the app. */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val s = WeatherCache(context).load()
            val views = RemoteViews(context.packageName, R.layout.widget_weather)
            views.setTextViewText(R.id.wCity, s.city)
            views.setTextViewText(R.id.wTemp, "${s.temp}${s.symbol}")
            views.setTextViewText(R.id.wCond, s.condition)

            val pi = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pi)
            manager.updateAppWidget(id, views)
        }
    }
}
