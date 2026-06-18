package com.nimboweather.forecast.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.ui.MainActivity

object Notifications {
    private const val CH_DAILY = "weather_daily"
    private const val CH_PERSIST = "weather_persistent"
    private const val CH_ALERT = "weather_alert"

    private const val ID_DAILY = 1001
    private const val ID_PERSIST = 2001
    private const val ID_ALERT = 3001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CH_DAILY, "Weather updates", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Daily forecast and weather changes" }
        )
        mgr.createNotificationChannel(
            NotificationChannel(CH_PERSIST, "Ongoing weather", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Always-on current weather in the status bar" }
        )
        mgr.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Severe weather alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Storms and severe conditions" }
        )
    }

    private fun contentPi(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canNotify(context: Context) =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** One-off daily update notification. */
    fun postDaily(context: Context, s: WeatherSnapshot) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CH_DAILY)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle("${s.city} · ${s.temp}${s.symbol}")
            .setContentText(s.condition)
            .setContentIntent(contentPi(context))
            .setAutoCancel(true)
            .build()
        safeNotify(context, ID_DAILY, n)
    }

    /** Persistent (ongoing) current-weather notification. enabled=false removes it. */
    fun updatePersistent(context: Context, s: WeatherSnapshot, enabled: Boolean) {
        if (!enabled) {
            NotificationManagerCompat.from(context).cancel(ID_PERSIST)
            return
        }
        if (!canNotify(context)) return
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CH_PERSIST)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle("${s.city} · ${s.temp}${s.symbol}")
            .setContentText(s.condition)
            .setContentIntent(contentPi(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        safeNotify(context, ID_PERSIST, n)
    }

    /** High-priority severe weather alert. */
    fun postAlert(context: Context, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CH_ALERT)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentPi(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        safeNotify(context, ID_ALERT, n)
    }

    private fun safeNotify(context: Context, id: Int, n: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — skip.
        }
    }
}
