package com.nimboweather.forecast.ui.radar

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nimboweather.forecast.BuildConfig
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.weathermap.RadarCoverage
import com.nimboweather.forecast.data.weathermap.WeatherLayer
import com.nimboweather.forecast.data.weathermap.WeatherTiles
import com.nimboweather.forecast.data.weathermap.point.PointRepository
import com.nimboweather.forecast.prefs.CityStore
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.util.Locale

/**
 * Multi-layer weather map: an Esri base with one static OWM/NEXRAD tile overlay for the active
 * layer. No time animation in v1 (forecast time axis needs paid OWM 2.0).
 */
class RadarActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val layerProvider by lazy { MapTileProviderBasic(applicationContext) }
    private var overlayAdded = false
    private var activeLayer = WeatherLayer.TEMP

    // Legend views (Task 9)
    private lateinit var legendBar: View
    private lateinit var legendMin: android.widget.TextView
    private lateinit var legendMid: android.widget.TextView
    private lateinit var legendMax: android.widget.TextView

    // Tap-to-query (Task 10)
    private val pointRepo = PointRepository()
    private var queryMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(
            applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_radar)
        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)
        map.setTileSource(esriBaseSource())

        // Capture legend views (Task 9)
        legendBar = findViewById(R.id.legendBar)
        legendMin = findViewById(R.id.legendMin)
        legendMid = findViewById(R.id.legendMid)
        legendMax = findViewById(R.id.legendMax)

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

        findViewById<View>(R.id.btnRadarBack).setOnClickListener { finish() }

        // Task 8: wire layer picker
        findViewById<View>(R.id.btnLayers).setOnClickListener { showLayerPicker() }

        // Task 10: tap-to-query events overlay — added at index 0 so it sits below other overlays
        val events = MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    queryPoint(p); return true
                }
                override fun longPressHelper(p: GeoPoint): Boolean = false
            }
        )
        map.overlays.add(0, events)

        showLayer(activeLayer)
    }

    /** Esri World_Topo_Map — z/y/x order + token, so a custom source (not plain XYTileSource). */
    private fun esriBaseSource(): OnlineTileSourceBase =
        object : OnlineTileSourceBase("EsriTopo", 0, ESRI_MAX_ZOOM, 256, ".png", arrayOf("")) {
            override fun getTileURLString(i: Long): String = WeatherTiles.esriUrl(
                z = MapTileIndex.getZoom(i), x = MapTileIndex.getX(i), y = MapTileIndex.getY(i),
                token = BuildConfig.ESRI_API_KEY
            )
        }

    /** Build the overlay tile source for [layer]: NEXRAD for RADAR, OWM model tiles otherwise. */
    private fun overlaySource(layer: WeatherLayer): OnlineTileSourceBase {
        if (layer == WeatherLayer.RADAR) {
            return object : OnlineTileSourceBase("nexrad", 0, MAX_ZOOM, TILE_SIZE, ".png", arrayOf("")) {
                override fun getTileURLString(i: Long): String = WeatherTiles.nexradUrl(
                    z = MapTileIndex.getZoom(i), x = MapTileIndex.getX(i), y = MapTileIndex.getY(i)
                )
            }
        }
        val owm = layer.owmLayer!! // safe: the RADAR branch returned above, so owmLayer is non-null here
        return object : OnlineTileSourceBase("owm_$owm", 0, MAX_ZOOM, TILE_SIZE, ".png", arrayOf("")) {
            override fun getTileURLString(i: Long): String = WeatherTiles.owmUrl(
                layer = owm, z = MapTileIndex.getZoom(i),
                x = MapTileIndex.getX(i), y = MapTileIndex.getY(i),
                key = BuildConfig.OPENWEATHER_API_KEY
            )
        }
    }

    private fun showLayer(layer: WeatherLayer) {
        activeLayer = layer
        layerProvider.tileSource = overlaySource(layer)
        layerProvider.clearTileCache() // drop the previous layer's cached tiles
        if (!overlayAdded) {
            val overlay = TilesOverlay(layerProvider, applicationContext).apply {
                loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                loadingLineColor = android.graphics.Color.TRANSPARENT
            }
            // Above the base map, below the city marker (the last overlay).
            map.overlays.add(map.overlays.size - 1, overlay)
            overlayAdded = true
        }
        map.invalidate()
        bindLegend(layer) // Task 9
    }

    // Task 8: layer picker dialog
    private fun showLayerPicker() {
        val layers = WeatherLayer.values()
        val labels = layers.map { getString(it.labelRes) }.toTypedArray()
        val checked = layers.indexOf(activeLayer)
        AlertDialog.Builder(this)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                selectLayer(layers[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun selectLayer(layer: WeatherLayer) {
        if (layer == WeatherLayer.RADAR) {
            val c = map.mapCenter
            // v1: gated at select-time on the current center only; panning out of coverage shows empty tiles.
            if (!RadarCoverage.hasNexrad(c.latitude, c.longitude)) {
                Toast.makeText(
                    this, R.string.radar_unavailable_here, Toast.LENGTH_SHORT
                ).show()
                showLayer(WeatherLayer.PRECIP)
                return
            }
        }
        showLayer(layer)
    }

    // Task 9: color-scale legend
    private fun bindLegend(layer: WeatherLayer) {
        val gd = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            layer.scaleColors
        ).apply {
            cornerRadius = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics
            )
        }
        legendBar.background = gd
        legendMin.text = "${layer.scaleMin}${layer.scaleUnit}"
        legendMid.text = "${(layer.scaleMin + layer.scaleMax) / 2}${layer.scaleUnit}"
        legendMax.text = "${layer.scaleMax}${layer.scaleUnit}"
    }

    // Task 10: tap-to-query
    private fun queryPoint(p: GeoPoint) {
        lifecycleScope.launch {
            val fallback = String.format(Locale.US, "%.2f, %.2f", p.latitude, p.longitude)
            val pf = pointRepo.query(p.latitude, p.longitude, activeLayer, fallback) ?: return@launch
            queryMarker?.let { map.overlays.remove(it) }
            val m = Marker(map).apply {
                position = p
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = pf.place
                snippet = pf.value
            }
            map.overlays.add(m)
            queryMarker = m
            m.showInfoWindow()
            map.invalidate()
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }

    companion object {
        private const val DEFAULT_LAT = 51.5074
        private const val DEFAULT_LON = -0.1278
        private const val DEFAULT_ZOOM = 6.0
        private const val MAX_ZOOM = 12
        private const val TILE_SIZE = 256
        private const val ESRI_MAX_ZOOM = 19
    }
}
