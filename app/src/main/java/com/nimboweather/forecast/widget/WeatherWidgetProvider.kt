package com.nimboweather.forecast.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.prefs.WidgetPrefs
import com.nimboweather.forecast.ui.MainActivity
import com.nimboweather.forecast.work.WidgetRefreshWorker

/** Home-screen widget. Each widget tracks a configured city (WidgetConfigActivity);
 *  falls back to the app's last-viewed city if not configured. */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            render(context, manager, id)
            enqueueRefresh(context, id)
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val prefs = WidgetPrefs(context)
        ids.forEach { prefs.clear(it) }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
                .forEach { render(context, manager, it) }
        }

        fun enqueueRefresh(context: Context, widgetId: Int) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                    .setInputData(workDataOf(WidgetRefreshWorker.KEY_WIDGET_ID to widgetId))
                    .build()
            )
        }

        fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val s = WidgetPrefs(context).getSnapshot(id) ?: WeatherCache(context).load()
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
