package com.nimboweather.forecast.data.radar

/**
 * Pure helpers over the RainViewer maps index: the ordered frame timeline and the per-tile
 * URL format. Frame-work free and unit tested; the map view supplies z/x/y per visible tile.
 *
 * Tile URL: `{host}{path}/{size}/{z}/{x}/{y}/{color}/{smooth}_{snow}.png`
 * (color = palette id, smooth = smoothing on/off, snow = show snow on/off).
 */
object RainViewerTiles {

    /** Past frames then nowcast frames, in chronological order, dropping blank-path entries. */
    fun frames(maps: RainViewerMaps): List<RadarFrame> {
        val radar = maps.radar ?: return emptyList()
        return (radar.past + radar.nowcast).filter { it.path.isNotEmpty() }
    }

    fun tileUrl(
        host: String,
        path: String,
        z: Int,
        x: Int,
        y: Int,
        size: Int = 256,
        color: Int = 2,
        smooth: Int = 1,
        snow: Int = 1
    ): String = "$host$path/$size/$z/$x/$y/$color/${smooth}_$snow.png"
}
