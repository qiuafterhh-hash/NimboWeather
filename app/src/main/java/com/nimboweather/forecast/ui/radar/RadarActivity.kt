package com.nimboweather.forecast.ui.radar

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.radar.RadarRepository
import com.nimboweather.forecast.data.radar.RadarTimeline
import com.nimboweather.forecast.data.radar.RainViewerTiles
import com.nimboweather.forecast.prefs.CityStore
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * L2 weather radar: an OpenStreetMap base with an animated RainViewer radar overlay,
 * centered on the selected city. RainViewer's free tier provides past/current frames only
 * (no future extrapolation) — see `docs/precip-nowcast-spec.md`.
 */
class RadarActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var timeLabel: TextView
    private lateinit var playBtn: ImageButton
    private lateinit var progress: ProgressBar

    private val radarProvider by lazy { MapTileProviderBasic(applicationContext) }
    private var radarOverlayAdded = false

    private var timeline: RadarTimeline = RadarTimeline("", emptyList())
    private var frameIndex = 0
    private var playing = true

    private val handler = Handler(Looper.getMainLooper())
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val advance = object : Runnable {
        override fun run() {
            if (timeline.frames.isEmpty()) return
            frameIndex = (frameIndex + 1) % timeline.frames.size
            showFrame(frameIndex)
            handler.postDelayed(this, FRAME_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // osmdroid must be configured (cache dirs + user agent) before the MapView inflates;
        // load() initializes the tile cache paths, without which no tiles ever download.
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_radar)
        map = findViewById(R.id.map)
        timeLabel = findViewById(R.id.tvRadarTime)
        playBtn = findViewById(R.id.btnRadarPlay)
        progress = findViewById(R.id.radarProgress)

        // Carto dark basemap — permitted for app embedding (OSM's own tile servers block
        // embedded apps) and a clean dark canvas for the radar overlay. No API key.
        map.setTileSource(
            XYTileSource(
                "CartoDark", 0, 20, 256, ".png",
                arrayOf(
                    "https://a.basemaps.cartocdn.com/dark_all/",
                    "https://b.basemaps.cartocdn.com/dark_all/",
                    "https://c.basemaps.cartocdn.com/dark_all/"
                )
            )
        )
        map.setMultiTouchControls(true)

        val city = CityStore(this).selected
        val center = GeoPoint(city?.lat ?: DEFAULT_LAT, city?.lon ?: DEFAULT_LON)
        map.controller.setZoom(DEFAULT_ZOOM)
        map.controller.setCenter(center)
        Marker(map).apply {
            position = center
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = city?.display()
            map.overlays.add(this)
        }

        findViewById<ImageButton>(R.id.btnRadarBack).setOnClickListener { finish() }
        playBtn.setOnClickListener { togglePlay() }

        loadTimeline()
    }

    private fun loadTimeline() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            timeline = RadarRepository().load()
            progress.visibility = View.GONE
            if (timeline.frames.isEmpty()) {
                timeLabel.setText(R.string.radar_unavailable)
                return@launch
            }
            frameIndex = timeline.frames.lastIndex // start on the most recent frame
            showFrame(frameIndex)
            if (playing) handler.postDelayed(advance, FRAME_MS)
        }
    }

    /** Point the (single) radar overlay at the given frame and update the timestamp label. */
    private fun showFrame(i: Int) {
        val frame = timeline.frames.getOrNull(i) ?: return
        val host = timeline.host
        // Unique source name per frame → osmdroid caches each frame separately, so after the
        // first loop the animation replays from cache without re-downloading.
        // Base-URL array must be non-empty or osmdroid treats the source as unavailable and
        // skips downloads; we override getTileURLString so the value itself is only a sentinel.
        radarProvider.tileSource = object : OnlineTileSourceBase(
            "rv_${frame.time}", 0, MAX_ZOOM, TILE_SIZE, ".png", arrayOf(host)
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String = RainViewerTiles.tileUrl(
                host = host,
                path = frame.path,
                z = MapTileIndex.getZoom(pMapTileIndex),
                x = MapTileIndex.getX(pMapTileIndex),
                y = MapTileIndex.getY(pMapTileIndex)
            )
        }

        if (!radarOverlayAdded) {
            val overlay = TilesOverlay(radarProvider, applicationContext).apply {
                loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                loadingLineColor = android.graphics.Color.TRANSPARENT
            }
            // Radar sits above the base map but below the city marker (the last overlay).
            map.overlays.add(map.overlays.size - 1, overlay)
            radarOverlayAdded = true
        }
        map.invalidate()
        timeLabel.text = timeFmt.format(Date(frame.time * 1000L))
    }

    private fun togglePlay() {
        playing = !playing
        if (playing) {
            playBtn.setImageResource(android.R.drawable.ic_media_pause)
            handler.postDelayed(advance, FRAME_MS)
        } else {
            playBtn.setImageResource(android.R.drawable.ic_media_play)
            handler.removeCallbacks(advance)
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        if (playing && timeline.frames.isNotEmpty()) handler.postDelayed(advance, FRAME_MS)
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        handler.removeCallbacks(advance)
    }

    companion object {
        private const val DEFAULT_LAT = 51.5074
        private const val DEFAULT_LON = -0.1278
        private const val DEFAULT_ZOOM = 6.0
        private const val MAX_ZOOM = 12
        private const val TILE_SIZE = 256
        private const val FRAME_MS = 600L
    }
}
