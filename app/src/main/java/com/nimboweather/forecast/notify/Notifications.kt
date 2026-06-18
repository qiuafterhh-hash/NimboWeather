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
    private const val CHANNEL = "weather_daily"
    private const val NOTIF_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "Weather updates", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Daily forecast and weather changes"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun postDaily(context: Context, s: WeatherSnapshot) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle("${s.city} · ${s.temp}${s.symbol}")
            .setContentText(s.condition)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted at runtime — skip silently.
        }
    }
}
