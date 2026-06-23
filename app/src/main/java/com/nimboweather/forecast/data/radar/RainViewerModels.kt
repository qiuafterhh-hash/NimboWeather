package com.nimboweather.forecast.data.radar

import kotlinx.serialization.Serializable

/**
 * RainViewer public weather-maps index (free, global, no key). Lists available radar frames;
 * each frame is a timestamp + a tile path slug. See `docs/precip-nowcast-spec.md` (L2).
 */
@Serializable
data class RainViewerMaps(
    val host: String = "",
    val radar: RainViewerRadar? = null
)

@Serializable
data class RainViewerRadar(
    val past: List<RadarFrame> = emptyList(),
    val nowcast: List<RadarFrame> = emptyList()
)

@Serializable
data class RadarFrame(
    val time: Long = 0,
    val path: String = ""
)
