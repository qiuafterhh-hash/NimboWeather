package com.nimboweather.forecast.ui.radar

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.nimboweather.forecast.BuildConfig
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.weathermap.WeatherLayer
import com.nimboweather.forecast.data.weathermap.WeatherTiles
import com.nimboweather.forecast.prefs.CityStore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

/**
 * Multi-layer weather map: an Esri base with one static OWM/NEXRAD tile overlay for the active
 * layer. No time animation in v1 (forecast time axis needs paid OWM 2.0).
 */
class RadarActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val layerProvider by lazy { MapTileProviderBasic(applicationContext) }
    private var overlayAdded = false
    private var activeLayer = WeatherLayer.TEMP

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

    /** OWM model-tile source for [layer]. RADAR is added in a later task; until then it is unused. */
    private fun overlaySource(layer: WeatherLayer): OnlineTileSourceBase {
        val owm = layer.owmLayer ?: WeatherLayer.PRECIP.owmLayer ?: "precipitation_new"
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
