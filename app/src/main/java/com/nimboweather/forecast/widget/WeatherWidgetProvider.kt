package com.nimboweather.forecast.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.AirQualityIndex
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.prefs.WidgetPrefs
import com.nimboweather.forecast.ui.MainActivity
import com.nimboweather.forecast.work.WidgetRefreshWorker

/** Home-screen widget. Each widget tracks a configured city (WidgetConfigActivity);
 *  falls back to the app's last-viewed city if not configured. Renders a small / medium
 *  / large layout depending on the widget's measured size. */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            render(context, manager, id)
            enqueueRefresh(context, id)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: android.os.Bundle?
    ) {
        // Re-render with the size-appropriate layout when the user resizes the widget.
        render(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (id >= 0) enqueueRefresh(context, id)
        }
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val prefs = WidgetPrefs(context)
        ids.forEach { prefs.clear(it) }
    }

    companion object {
        private const val ACTION_REFRESH = "com.nimboweather.forecast.widget.ACTION_REFRESH"

        /** Re-render every live widget from its cached snapshot (no network). */
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

        /** Re-fetch every live widget's configured city. Called from the periodic
         *  WeatherWorker so configured widgets update on the WorkManager cadence,
         *  not just the system's hourly onUpdate. */
        fun enqueueRefreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
                .forEach { enqueueRefresh(context, it) }
        }

        fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val s = WidgetPrefs(context).getSnapshot(id) ?: WeatherCache(context).load()
            val layout = chooseLayout(manager.getAppWidgetOptions(id))
            val views = RemoteViews(context.packageName, layout)
            bind(context, views, s, id)
            manager.updateAppWidget(id, views)
        }

        /** dp size buckets → layout. minWidth/minHeight are portrait-ish bounds. */
        private fun chooseLayout(options: android.os.Bundle?): Int {
            val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
            val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            return when {
                minW >= 250 && minH >= 180 -> R.layout.widget_weather_large
                minH < 110 -> R.layout.widget_weather_small
                else -> R.layout.widget_weather_medium
            }
        }

        private fun bind(context: Context, views: RemoteViews, s: WeatherSnapshot, id: Int) {
            val degree = s.symbol.firstOrNull()?.toString() ?: "°" // "°C" → "°"

            views.setTextViewText(R.id.wCity, s.city)
            views.setTextViewText(R.id.wTemp, "${s.temp}${s.symbol}")
            views.setTextViewText(R.id.wCond, s.condition)
            views.setImageViewResource(R.id.wGlyph, WidgetVisuals.glyph(s.icon))
            views.setInt(R.id.widgetRoot, "setBackgroundResource", WidgetVisuals.background(s.icon))

            // hi/lo + feels-like (medium/large)
            val hiLo = buildString {
                if (s.hi != null && s.lo != null) append("H:${s.hi}$degree  L:${s.lo}$degree")
                if (s.feelsLike != null) {
                    if (isNotEmpty()) append("   ")
                    append("Feels ${s.feelsLike}$degree")
                }
            }
            views.setTextViewText(R.id.wHiLo, hiLo)

            // AQI chip (large)
            if (s.aqi != null) {
                views.setTextViewText(R.id.wAqi, "AQI ${s.aqi} · ${AirQualityIndex.category(s.aqi)}")
                views.setViewVisibility(R.id.wAqi, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.wAqi, View.GONE)
            }

            // 3-hour strip (large only). Hide whole row if no hourly data.
            bindStrip(views, s, degree)

            // freshness
            val now = System.currentTimeMillis()
            views.setTextViewText(R.id.wUpdated, WidgetFormat.updatedLabel(now, s.updatedAt))
            views.setTextColor(
                R.id.wUpdated,
                if (WidgetFormat.isStale(now, s.updatedAt)) STALE_COLOR else FRESH_COLOR
            )

            // tap whole widget → app; tap refresh icon (large) → re-fetch
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            views.setOnClickPendingIntent(R.id.wRefresh, refreshIntent(context, id))
        }

        private val SLOT_IDS = arrayOf(
            intArrayOf(R.id.wSlot0, R.id.wSlot0Time, R.id.wSlot0Glyph, R.id.wSlot0Temp),
            intArrayOf(R.id.wSlot1, R.id.wSlot1Time, R.id.wSlot1Glyph, R.id.wSlot1Temp),
            intArrayOf(R.id.wSlot2, R.id.wSlot2Time, R.id.wSlot2Glyph, R.id.wSlot2Temp)
        )

        private fun bindStrip(views: RemoteViews, s: WeatherSnapshot, degree: String) {
            views.setViewVisibility(R.id.wStrip, if (s.hours.isEmpty()) View.GONE else View.VISIBLE)
            SLOT_IDS.forEachIndexed { i, ids ->
                val slot = s.hours.getOrNull(i)
                if (slot == null) {
                    views.setViewVisibility(ids[0], View.INVISIBLE)
                } else {
                    views.setViewVisibility(ids[0], View.VISIBLE)
                    views.setTextViewText(ids[1], slot.label)
                    views.setImageViewResource(ids[2], WidgetVisuals.glyph(slot.icon))
                    views.setTextViewText(ids[3], "${slot.temp}$degree")
                }
            }
        }

        private const val STALE_COLOR = 0xFFFFD08A.toInt() // amber
        private const val FRESH_COLOR = 0x99FFFFFF.toInt() // muted white

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun refreshIntent(context: Context, id: Int): PendingIntent {
            val intent = Intent(context, WeatherWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            return PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
