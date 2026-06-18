package com.nimboweather.forecast.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Best-effort last-known location. Returns null if no permission / unavailable. */
    @SuppressLint("MissingPermission")
    suspend fun lastKnown(): Pair<Double, Double>? {
        if (!hasPermission()) return null
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val loc = client.lastLocation.await() ?: return null
            loc.latitude to loc.longitude
        } catch (e: Exception) {
            null
        }
    }
}
