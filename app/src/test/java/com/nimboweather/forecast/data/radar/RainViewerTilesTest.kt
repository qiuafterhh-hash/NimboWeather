package com.nimboweather.forecast.data.radar

import org.junit.Assert.assertEquals
import org.junit.Test

class RainViewerTilesTest {

    // --- frames(): order past → nowcast, drop blanks ------------------------

    @Test fun frames_combine_past_then_nowcast_in_order() {
        val maps = RainViewerMaps(
            host = "https://t.rainviewer.com",
            radar = RainViewerRadar(
                past = listOf(RadarFrame(100, "/p1"), RadarFrame(200, "/p2")),
                nowcast = listOf(RadarFrame(300, "/n1"))
            )
        )
        assertEquals(
            listOf(RadarFrame(100, "/p1"), RadarFrame(200, "/p2"), RadarFrame(300, "/n1")),
            RainViewerTiles.frames(maps)
        )
    }

    @Test fun frames_drop_blank_paths() {
        val maps = RainViewerMaps(
            radar = RainViewerRadar(past = listOf(RadarFrame(1, "/ok"), RadarFrame(2, "")))
        )
        assertEquals(listOf(RadarFrame(1, "/ok")), RainViewerTiles.frames(maps))
    }

    @Test fun frames_empty_when_radar_null() {
        assertEquals(emptyList<RadarFrame>(), RainViewerTiles.frames(RainViewerMaps()))
    }

    // --- tileUrl(): {host}{path}/{size}/{z}/{x}/{y}/{color}/{smooth}_{snow}.png

    @Test fun tile_url_is_built_in_rainviewer_format() {
        assertEquals(
            "https://t.rainviewer.com/v2/radar/abc/256/3/4/2/2/1_1.png",
            RainViewerTiles.tileUrl("https://t.rainviewer.com", "/v2/radar/abc", z = 3, x = 4, y = 2)
        )
    }

    @Test fun tile_url_honors_overrides() {
        assertEquals(
            "https://h/p/512/7/1/2/4/0_0.png",
            RainViewerTiles.tileUrl("https://h", "/p", z = 7, x = 1, y = 2, size = 512, color = 4, smooth = 0, snow = 0)
        )
    }
}
