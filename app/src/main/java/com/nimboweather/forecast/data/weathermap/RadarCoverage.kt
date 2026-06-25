package com.nimboweather.forecast.data.weathermap

/**
 * Whether a point has free, commercially-usable ground-radar coverage. v1 ships only the US
 * NEXRAD mosaic (IEM), so coverage is the contiguous-US bounding box. Emerging-market points
 * (the app's primary audience) return false → the UI falls back to the precipitation layer.
 * Extension point for v2: add more covered regions / a paid global source here.
 */
object RadarCoverage {
    fun hasNexrad(lat: Double, lon: Double): Boolean =
        lat in 24.0..50.0 && lon in -125.0..-66.0
}
