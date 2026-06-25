# Weather Map Layers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the experimental RainViewer "rain radar" screen into a multi-layer weather map (temperature / precipitation / wind, plus a coverage-gated radar entry) with a layer switcher, a color-scale legend, and tap-to-query popups — using only free, commercially-usable data sources that cover emerging markets.

**Architecture:** Keep osmdroid as the map engine. Replace the non-commercial Carto base map with Esri's free commercial basemap. Each weather layer is a single **static** tile overlay (no time animation in v1) sourced from OpenWeatherMap's free Weather Maps tiles (model-based, global coverage). The "radar" entry is gated by a US bounding box: inside it shows free public NEXRAD tiles (IEM), outside it falls back to the precipitation layer with a notice. Tap-to-query calls OWM's free Current Weather point endpoint (reusing the existing key, consistent with the heatmap) and shows a popup. The old RainViewer timeline/animation code is deleted.

**Tech Stack:** Kotlin, osmdroid 6.1.18, Retrofit 2.11 + kotlinx.serialization, OpenWeatherMap Weather Maps tiles + Current Weather API, Esri ArcGIS Location Platform basemap, IEM NEXRAD tiles. JUnit for pure-logic unit tests; build + on-device run for osmdroid/UI verification.

**Key decisions locked during brainstorming:**
- Target market = overseas/emerging. No free commercial ground-radar source covers it → "radar" is coverage-gated, not a primary layer.
- v1 = 3 model layers + tap query + base-map swap. **No** forecast time axis (needs paid OWM 2.0) and **no** city temperature labels (deferred to v2).
- Tap-query uses OWM Current Weather (not MET Norway) — same provider as the heatmap, reuses the key, returns city name.

**Data source reference (verified 2026-06):**
- OWM tiles: `https://tile.openweathermap.org/map/{layer}/{z}/{x}/{y}.png?appid={KEY}` — layers `temp_new`, `precipitation_new`, `wind_new`. Free tier, global, commercial OK, **must credit OpenWeather**.
- OWM point: `https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&units=metric&appid={KEY}` — returns `name`, `dt`, `main.temp`, `wind.speed`, `rain.1h`.
- Esri base: `https://ibasemaps-api.arcgis.com/arcgis/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}?token={KEY}` — **z/y/x order**, free 2M tiles/mo, commercial OK.
- IEM NEXRAD (US radar): `https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/nexrad-n0q-900913/{z}/{x}/{y}.png` — standard z/x/y, public, transparent PNG.

---

## File Structure

**New files:**
- `app/src/main/java/com/nimboweather/forecast/data/weathermap/WeatherLayer.kt` — enum of layers (id, OWM tile layer string, display string res, color-scale stops, min/max). Pure.
- `app/src/main/java/com/nimboweather/forecast/data/weathermap/WeatherTiles.kt` — pure tile-URL builders for OWM layers and IEM NEXRAD. Unit tested.
- `app/src/main/java/com/nimboweather/forecast/data/weathermap/RadarCoverage.kt` — pure US-bbox coverage check. Unit tested.
- `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointApi.kt` — Retrofit interface for OWM current weather.
- `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointModels.kt` — response DTOs.
- `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointRetrofit.kt` — Retrofit instance (OWM host).
- `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/PointForecast.kt` — pure mapper: response + active layer → popup text. Unit tested.
- `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/PointRepository.kt` — fetch wrapper, degrades to null.
- `app/src/test/java/com/nimboweather/forecast/data/weathermap/WeatherTilesTest.kt`
- `app/src/test/java/com/nimboweather/forecast/data/weathermap/RadarCoverageTest.kt`
- `app/src/test/java/com/nimboweather/forecast/data/weathermap/point/PointForecastTest.kt`

**Modified files:**
- `app/build.gradle.kts` — add `ESRI_API_KEY` buildConfigField.
- `app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt` — full rewrite of overlay logic.
- `app/src/main/res/layout/activity_radar.xml` — replace play/pause bottom bar with layer button + legend.
- `app/src/main/res/values/strings.xml` — new strings, retire radar-animation strings.

**Deleted files (dead after refactor — RainViewer is non-commercial as of 2026-01):**
- `app/src/main/java/com/nimboweather/forecast/data/radar/RainViewerApi.kt`
- `app/src/main/java/com/nimboweather/forecast/data/radar/RainViewerModels.kt`
- `app/src/main/java/com/nimboweather/forecast/data/radar/RainViewerTiles.kt`
- `app/src/test/java/com/nimboweather/forecast/data/radar/RainViewerTilesTest.kt`

---

## Task 1: Add Esri API key build config

**Files:**
- Modify: `app/build.gradle.kts:14` and `:50`

- [ ] **Step 1: Add the Esri key property + buildConfigField**

In `app/build.gradle.kts`, after line 14 (`val owmKey = ...`):

```kotlin
val esriKey: String = localProps.getProperty("ESRI_API_KEY") ?: ""
```

In `defaultConfig`, after the existing `buildConfigField("String", "OPENWEATHER_API_KEY", ...)` line:

```kotlin
buildConfigField("String", "ESRI_API_KEY", "\"$esriKey\"")
```

- [ ] **Step 2: Document the key in local.properties (developer machine only)**

Add to `local.properties` (gitignored — do NOT commit a real key):

```
ESRI_API_KEY=your_esri_location_platform_key
```

- [ ] **Step 3: Build to confirm BuildConfig generates**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. `BuildConfig.ESRI_API_KEY` now resolvable.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: add ESRI_API_KEY build config for weather-map basemap"
```

---

## Task 2: WeatherLayer model

**Files:**
- Create: `app/src/main/java/com/nimboweather/forecast/data/weathermap/WeatherLayer.kt`
- Test: `app/src/test/java/com/nimboweather/forecast/data/weathermap/WeatherTilesTest.kt` (layer assertions added here to avoid a separate file)

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/nimboweather/forecast/data/weathermap/WeatherTilesTest.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherLayerTest {
    @Test fun temp_precip_wind_carry_owm_layer_ids() {
        assertEquals("temp_new", WeatherLayer.TEMP.owmLayer)
        assertEquals("precipitation_new", WeatherLayer.PRECIP.owmLayer)
        assertEquals("wind_new", WeatherLayer.WIND.owmLayer)
    }

    @Test fun radar_has_no_owm_layer() {
        assertNull(WeatherLayer.RADAR.owmLayer)
    }

    @Test fun color_scale_min_below_max() {
        WeatherLayer.values().forEach {
            assert(it.scaleMin < it.scaleMax) { "${it.name} scale invalid" }
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*WeatherLayerTest*"`
Expected: FAIL — `WeatherLayer` unresolved.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/nimboweather/forecast/data/weathermap/WeatherLayer.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap

import androidx.annotation.StringRes
import com.nimboweather.forecast.R

/**
 * A selectable weather-map layer. [owmLayer] is the OpenWeatherMap tile layer id for model-based
 * layers (global coverage); RADAR has none — it is sourced from coverage-gated NEXRAD tiles and
 * falls back to PRECIP outside the covered region. [scaleMin]/[scaleMax]/[scaleColors] describe the
 * legend gradient only (the tile pixels are colored by the provider).
 */
enum class WeatherLayer(
    @StringRes val labelRes: Int,
    val owmLayer: String?,
    val scaleMin: Int,
    val scaleMax: Int,
    val scaleUnit: String,
    val scaleColors: IntArray,
) {
    TEMP(
        R.string.layer_temp, "temp_new", -20, 40, "°C",
        intArrayOf(0xFF4A2DB5.toInt(), 0xFF2E9BE6.toInt(), 0xFF49D49D.toInt(),
            0xFFE8E84A.toInt(), 0xFFE8862E.toInt(), 0xFFD53A2D.toInt())
    ),
    PRECIP(
        R.string.layer_precip, "precipitation_new", 0, 50, "mm",
        intArrayOf(0x00FFFFFF, 0xFF9BD3F0.toInt(), 0xFF3A86E8.toInt(), 0xFF6B3AE8.toInt())
    ),
    WIND(
        R.string.layer_wind, "wind_new", 0, 30, "m/s",
        intArrayOf(0xFF49D49D.toInt(), 0xFFE8E84A.toInt(), 0xFFE8862E.toInt(), 0xFFD53A2D.toInt())
    ),
    RADAR(
        R.string.layer_radar, null, 0, 70, "dBZ",
        intArrayOf(0xFF49D49D.toInt(), 0xFFE8E84A.toInt(), 0xFFE8862E.toInt(), 0xFFD53A2D.toInt())
    );
}
```

- [ ] **Step 4: Add the string resources used above**

In `app/src/main/res/values/strings.xml`, add:

```xml
<string name="layer_temp">Temperature</string>
<string name="layer_precip">Precipitation</string>
<string name="layer_wind">Wind</string>
<string name="layer_radar">Radar</string>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*WeatherLayerTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nimboweather/forecast/data/weathermap/WeatherLayer.kt \
        app/src/test/java/com/nimboweather/forecast/data/weathermap/WeatherTilesTest.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(weathermap): add WeatherLayer model"
```

---

## Task 3: Tile URL builders (WeatherTiles)

**Files:**
- Create: `app/src/main/java/com/nimboweather/forecast/data/weathermap/WeatherTiles.kt`
- Test: `app/src/test/java/com/nimboweather/forecast/data/weathermap/WeatherTilesTest.kt` (append)

- [ ] **Step 1: Append failing tests**

Add to `WeatherTilesTest.kt`:

```kotlin
class WeatherTilesUrlTest {
    @Test fun owm_url_has_layer_zxy_and_key() {
        val url = WeatherTiles.owmUrl("temp_new", z = 5, x = 3, y = 7, key = "KEY123")
        assertEquals(
            "https://tile.openweathermap.org/map/temp_new/5/3/7.png?appid=KEY123", url
        )
    }

    @Test fun esri_url_uses_z_y_x_order_with_token() {
        val url = WeatherTiles.esriUrl(z = 5, x = 3, y = 7, token = "TOK")
        assertEquals(
            "https://ibasemaps-api.arcgis.com/arcgis/rest/services/World_Topo_Map/" +
                "MapServer/tile/5/7/3?token=TOK", url
        )
    }

    @Test fun nexrad_url_is_zxy() {
        val url = WeatherTiles.nexradUrl(z = 5, x = 3, y = 7)
        assertEquals(
            "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/" +
                "nexrad-n0q-900913/5/3/7.png", url
        )
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*WeatherTilesUrlTest*"`
Expected: FAIL — `WeatherTiles` unresolved.

- [ ] **Step 3: Implement**

Create `app/src/main/java/com/nimboweather/forecast/data/weathermap/WeatherTiles.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap

/**
 * Pure tile-URL builders. The map view supplies z/x/y per visible tile. Note Esri uses z/y/x
 * order (unlike the XYZ standard), so it cannot use osmdroid's plain XYTileSource.
 */
object WeatherTiles {

    fun owmUrl(layer: String, z: Int, x: Int, y: Int, key: String): String =
        "https://tile.openweathermap.org/map/$layer/$z/$x/$y.png?appid=$key"

    fun esriUrl(z: Int, x: Int, y: Int, token: String): String =
        "https://ibasemaps-api.arcgis.com/arcgis/rest/services/World_Topo_Map/" +
            "MapServer/tile/$z/$y/$x?token=$token"

    fun nexradUrl(z: Int, x: Int, y: Int): String =
        "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/" +
            "nexrad-n0q-900913/$z/$x/$y.png"
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*WeatherTilesUrlTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nimboweather/forecast/data/weathermap/WeatherTiles.kt \
        app/src/test/java/com/nimboweather/forecast/data/weathermap/WeatherTilesTest.kt
git commit -m "feat(weathermap): add tile URL builders for OWM/Esri/NEXRAD"
```

---

## Task 4: Radar coverage check

**Files:**
- Create: `app/src/main/java/com/nimboweather/forecast/data/weathermap/RadarCoverage.kt`
- Test: `app/src/test/java/com/nimboweather/forecast/data/weathermap/RadarCoverageTest.kt`

- [ ] **Step 1: Write failing test**

Create `app/src/test/java/com/nimboweather/forecast/data/weathermap/RadarCoverageTest.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarCoverageTest {
    @Test fun us_point_is_covered() {
        // Kansas City, central US
        assertTrue(RadarCoverage.hasNexrad(lat = 39.1, lon = -94.6))
    }

    @Test fun jakarta_is_not_covered() {
        assertFalse(RadarCoverage.hasNexrad(lat = -6.2, lon = 106.8))
    }

    @Test fun london_is_not_covered() {
        assertFalse(RadarCoverage.hasNexrad(lat = 51.5, lon = -0.13))
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*RadarCoverageTest*"`
Expected: FAIL — `RadarCoverage` unresolved.

- [ ] **Step 3: Implement**

Create `app/src/main/java/com/nimboweather/forecast/data/weathermap/RadarCoverage.kt`:

```kotlin
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
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*RadarCoverageTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nimboweather/forecast/data/weathermap/RadarCoverage.kt \
        app/src/test/java/com/nimboweather/forecast/data/weathermap/RadarCoverageTest.kt
git commit -m "feat(weathermap): add US NEXRAD coverage check"
```

---

## Task 5: OWM point query (data layer)

**Files:**
- Create: `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointModels.kt`
- Create: `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointApi.kt`
- Create: `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointRetrofit.kt`
- Create: `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/PointForecast.kt`
- Create: `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/PointRepository.kt`
- Test: `app/src/test/java/com/nimboweather/forecast/data/weathermap/point/PointForecastTest.kt`

- [ ] **Step 1: Write failing test for the pure mapper**

Create `app/src/test/java/com/nimboweather/forecast/data/weathermap/point/PointForecastTest.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap.point

import com.nimboweather.forecast.data.weathermap.WeatherLayer
import org.junit.Assert.assertEquals
import org.junit.Test

class PointForecastTest {
    private val resp = OwmPointResponse(
        name = "Jakarta",
        dt = 1_700_000_000L,
        main = OwmMain(temp = 27.4),
        wind = OwmWind(speed = 3.2),
        rain = OwmRain(oneHour = 1.5)
    )

    @Test fun temp_layer_shows_temperature() {
        val p = PointForecast.from(resp, WeatherLayer.TEMP)
        assertEquals("Jakarta", p.place)
        assertEquals("27°C", p.value)
    }

    @Test fun wind_layer_shows_wind_speed() {
        assertEquals("3 m/s", PointForecast.from(resp, WeatherLayer.WIND).value)
    }

    @Test fun precip_layer_shows_rain() {
        assertEquals("1.5 mm", PointForecast.from(resp, WeatherLayer.PRECIP).value)
    }

    @Test fun missing_rain_renders_zero() {
        val dry = resp.copy(rain = null)
        assertEquals("0 mm", PointForecast.from(dry, WeatherLayer.PRECIP).value)
    }

    @Test fun blank_name_falls_back_to_coords_placeholder() {
        val p = PointForecast.from(resp.copy(name = ""), WeatherLayer.TEMP, fallbackPlace = "—")
        assertEquals("—", p.place)
    }
}
```

- [ ] **Step 2: Run to verify fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*PointForecastTest*"`
Expected: FAIL — types unresolved.

- [ ] **Step 3: Implement the models**

Create `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointModels.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap.point

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OwmPointResponse(
    val name: String = "",
    val dt: Long = 0L,
    val main: OwmMain = OwmMain(),
    val wind: OwmWind = OwmWind(),
    val rain: OwmRain? = null,
)

@Serializable
data class OwmMain(val temp: Double = 0.0)

@Serializable
data class OwmWind(val speed: Double = 0.0)

@Serializable
data class OwmRain(@SerialName("1h") val oneHour: Double = 0.0)
```

- [ ] **Step 4: Implement the pure mapper**

Create `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/PointForecast.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap.point

import com.nimboweather.forecast.data.weathermap.WeatherLayer
import kotlin.math.roundToInt

/** A resolved popup: a place name and the value of the active layer at that point. */
data class PointForecast(val place: String, val value: String) {
    companion object {
        fun from(
            resp: OwmPointResponse,
            layer: WeatherLayer,
            fallbackPlace: String = ""
        ): PointForecast {
            val place = resp.name.ifBlank { fallbackPlace }
            val value = when (layer) {
                WeatherLayer.WIND -> "${resp.wind.speed.roundToInt()} m/s"
                WeatherLayer.PRECIP, WeatherLayer.RADAR -> {
                    val mm = resp.rain?.oneHour ?: 0.0
                    if (mm == 0.0) "0 mm" else "$mm mm"
                }
                WeatherLayer.TEMP -> "${resp.main.temp.roundToInt()}°C"
            }
            return PointForecast(place, value)
        }
    }
}
```

- [ ] **Step 5: Run mapper test to verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*PointForecastTest*"`
Expected: PASS.

- [ ] **Step 6: Implement the API + Retrofit + repository (no unit test — network)**

Create `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointApi.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap.point

import retrofit2.http.GET
import retrofit2.http.Query

/** OWM Current Weather — point value at a lat/lon (free tier, consistent with the heatmap tiles). */
interface OwmPointApi {
    @GET("data/2.5/weather")
    suspend fun current(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") appid: String,
    ): OwmPointResponse
}
```

Create `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/OwmPointRetrofit.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap.point

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object OwmPointRetrofit {
    private const val BASE_URL = "https://api.openweathermap.org/"
    private val json = Json { ignoreUnknownKeys = true }

    val api: OwmPointApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OwmPointApi::class.java)
    }
}
```

Create `app/src/main/java/com/nimboweather/forecast/data/weathermap/point/PointRepository.kt`:

```kotlin
package com.nimboweather.forecast.data.weathermap.point

import com.nimboweather.forecast.BuildConfig
import com.nimboweather.forecast.data.weathermap.WeatherLayer

/** Fetches a point forecast; network/parse failures degrade to null (caller shows nothing). */
class PointRepository(private val api: OwmPointApi = OwmPointRetrofit.api) {
    suspend fun query(
        lat: Double,
        lon: Double,
        layer: WeatherLayer,
        fallbackPlace: String
    ): PointForecast? = runCatching {
        val resp = api.current(lat = lat, lon = lon, appid = BuildConfig.OPENWEATHER_API_KEY)
        PointForecast.from(resp, layer, fallbackPlace)
    }.getOrNull()
}
```

- [ ] **Step 7: Build + run all weathermap tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.nimboweather.forecast.data.weathermap.*"`
Expected: PASS (all WeatherLayer / WeatherTiles / RadarCoverage / PointForecast tests).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/nimboweather/forecast/data/weathermap/point \
        app/src/test/java/com/nimboweather/forecast/data/weathermap/point
git commit -m "feat(weathermap): OWM point-query data layer + pure mapper"
```

---

## Task 6: Tile-source factory in the activity (Esri base + layer overlay)

This task swaps the base map to Esri and introduces a helper to build per-layer overlay tile sources. It replaces the RainViewer/Carto wiring in `RadarActivity`. No timeline/animation.

**Files:**
- Modify: `app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt` (full rewrite — see Task 7 for the complete file; this task lands the base map + a single TEMP overlay so we can verify rendering early)

- [ ] **Step 1: Rewrite RadarActivity with Esri base + a single TEMP overlay (no animation)**

Replace the entire contents of `app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt`:

```kotlin
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
    private var layerOverlay: TilesOverlay? = null
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
        object : OnlineTileSourceBase("EsriTopo", 0, 19, 256, ".png", arrayOf("")) {
            override fun getTileURLString(i: Long): String = WeatherTiles.esriUrl(
                z = MapTileIndex.getZoom(i), x = MapTileIndex.getX(i), y = MapTileIndex.getY(i),
                token = BuildConfig.ESRI_API_KEY
            )
        }

    /** Build the overlay tile source for [layer] (OWM model tiles; RADAR handled in Task 8). */
    private fun overlaySource(layer: WeatherLayer): OnlineTileSourceBase {
        val owm = layer.owmLayer ?: WeatherLayer.PRECIP.owmLayer!!
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
        // Remove the previous overlay so the provider/cache for the old layer is released.
        layerOverlay?.let { map.overlays.remove(it) }
        val provider = MapTileProviderBasic(applicationContext).apply {
            tileSource = overlaySource(layer)
        }
        val overlay = TilesOverlay(provider, applicationContext).apply {
            loadingBackgroundColor = android.graphics.Color.TRANSPARENT
            loadingLineColor = android.graphics.Color.TRANSPARENT
        }
        // Above base, below the city marker (last overlay).
        map.overlays.add(map.overlays.size - 1, overlay)
        layerOverlay = overlay
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
    }
}
```

- [ ] **Step 2: Build (will fail until layout/strings updated in Task 7 — expected)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: may FAIL on `R.id.btnRadarPlay`/removed refs only if layout still references them — the layout is updated in Task 7. If compile passes, continue; if it fails on missing R.id, proceed to Task 7 then build.

- [ ] **Step 3: Commit (WIP — base map + single overlay)**

```bash
git add app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt
git commit -m "refactor(weathermap): Esri base + static layer overlay (drops RainViewer animation)"
```

---

## Task 7: Update layout + strings; verify tiles render on device

**Files:**
- Modify: `app/src/main/res/layout/activity_radar.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Replace the bottom bar (play/pause → layer button + legend container)**

Replace the bottom `LinearLayout` (the play/pause bar, lines ~42-78) in `activity_radar.xml` with:

```xml
    <!-- Top bar: back + title + layer switcher -->
    <!-- (top bar already present above; ADD the layer button into it) -->

    <!-- Bottom: color-scale legend + attribution -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:background="#CC0B1A2E"
        android:orientation="vertical"
        android:padding="10dp">

        <include
            android:id="@+id/legend"
            layout="@layout/view_layer_legend" />

        <TextView
            android:id="@+id/tvAttribution"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:alpha="0.7"
            android:text="@string/weathermap_attribution"
            android:textColor="@color/on_sky_secondary"
            android:textSize="10sp" />
    </LinearLayout>
```

In the **top** bar `LinearLayout`, after the title `TextView`, add the layer button:

```xml
        <ImageButton
            android:id="@+id/btnLayers"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/layer_switch"
            android:src="@drawable/ic_map"
            app:tint="@color/on_sky" />
```

- [ ] **Step 2: Create the legend layout**

Create `app/src/main/res/layout/view_layer_legend.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <View
        android:id="@+id/legendBar"
        android:layout_width="match_parent"
        android:layout_height="8dp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:weightSum="3">

        <TextView android:id="@+id/legendMin" android:layout_width="0dp"
            android:layout_weight="1" android:layout_height="wrap_content"
            android:textColor="@color/on_sky" android:textSize="10sp" android:gravity="start" />
        <TextView android:id="@+id/legendMid" android:layout_width="0dp"
            android:layout_weight="1" android:layout_height="wrap_content"
            android:textColor="@color/on_sky" android:textSize="10sp" android:gravity="center" />
        <TextView android:id="@+id/legendMax" android:layout_width="0dp"
            android:layout_weight="1" android:layout_height="wrap_content"
            android:textColor="@color/on_sky" android:textSize="10sp" android:gravity="end" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: Update strings (retire animation strings, add new)**

In `app/src/main/res/values/strings.xml`: change `radar_title` and remove the now-unused animation strings; add new ones:

```xml
<string name="radar_title">Weather map</string>
<string name="weathermap_attribution">© OpenWeather · Esri · NEXRAD/IEM</string>
<string name="layer_switch">Switch layer</string>
<string name="radar_unavailable_here">No radar coverage here — showing precipitation</string>
```

Delete: `radar_play_pause`, `radar_attribution`, `radar_unavailable` (replaced).

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run on device and verify the temperature overlay renders**

Install + launch, open the weather map (top-bar map icon on the home screen). Use the project's run skill or:
Run: `./gradlew :app:installDebug` then launch `RadarActivity` via the home-screen map button.
Expected: Esri topo base map renders; a translucent temperature heatmap overlay is visible over it (requires valid `OPENWEATHER_API_KEY` and `ESRI_API_KEY` in `local.properties`).
**If the overlay does NOT render:** STOP and use superpowers:systematic-debugging — likely causes: (a) empty `ESRI_API_KEY`/`OPENWEATHER_API_KEY`, (b) `MapTileProviderBasic` not invalidating — call `map.tileProvider.clearTileCache()` is NOT needed since each overlay has its own provider; verify `getTileURLString` returns a 200 by logging the URL and curling it.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/layout/activity_radar.xml \
        app/src/main/res/layout/view_layer_legend.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat(weathermap): legend + layer-button layout, weather-map strings"
```

---

## Task 8: Layer switcher + radar coverage gating

**Files:**
- Modify: `app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt`

- [ ] **Step 1: Add the layer picker dialog + RADAR gating**

In `RadarActivity`, wire the layer button in `onCreate` (after `btnRadarBack`):

```kotlin
        findViewById<View>(R.id.btnLayers).setOnClickListener { showLayerPicker() }
```

Add methods:

```kotlin
    private fun showLayerPicker() {
        val layers = WeatherLayer.values()
        val labels = layers.map { getString(it.labelRes) }.toTypedArray()
        val checked = layers.indexOf(activeLayer)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                selectLayer(layers[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun selectLayer(layer: WeatherLayer) {
        if (layer == WeatherLayer.RADAR) {
            val c = map.mapCenter
            if (!com.nimboweather.forecast.data.weathermap.RadarCoverage
                    .hasNexrad(c.latitude, c.longitude)) {
                android.widget.Toast.makeText(
                    this, R.string.radar_unavailable_here, android.widget.Toast.LENGTH_SHORT
                ).show()
                showLayer(WeatherLayer.PRECIP)
                return
            }
        }
        showLayer(layer)
    }
```

- [ ] **Step 2: Make `overlaySource` serve NEXRAD for the RADAR layer**

Replace `overlaySource` in `RadarActivity`:

```kotlin
    private fun overlaySource(layer: WeatherLayer): OnlineTileSourceBase {
        if (layer == WeatherLayer.RADAR) {
            return object : OnlineTileSourceBase("nexrad", 0, MAX_ZOOM, TILE_SIZE, ".png", arrayOf("")) {
                override fun getTileURLString(i: Long): String = WeatherTiles.nexradUrl(
                    z = MapTileIndex.getZoom(i), x = MapTileIndex.getX(i), y = MapTileIndex.getY(i)
                )
            }
        }
        val owm = layer.owmLayer!!
        return object : OnlineTileSourceBase("owm_$owm", 0, MAX_ZOOM, TILE_SIZE, ".png", arrayOf("")) {
            override fun getTileURLString(i: Long): String = WeatherTiles.owmUrl(
                layer = owm, z = MapTileIndex.getZoom(i),
                x = MapTileIndex.getX(i), y = MapTileIndex.getY(i),
                key = BuildConfig.OPENWEATHER_API_KEY
            )
        }
    }
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run + verify switching**

Launch, tap the layer button → pick Precipitation/Wind → overlay changes. Pick Radar from a non-US center → toast "No radar coverage here — showing precipitation" + precip overlay shown.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt
git commit -m "feat(weathermap): layer picker + radar coverage gating"
```

---

## Task 9: Color-scale legend driven by active layer

**Files:**
- Modify: `app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt`

- [ ] **Step 1: Bind the legend in `showLayer`**

Add fields + a bind call. In `onCreate` after `setContentView`, capture the legend views:

```kotlin
        legendBar = findViewById(R.id.legendBar)
        legendMin = findViewById(R.id.legendMin)
        legendMid = findViewById(R.id.legendMid)
        legendMax = findViewById(R.id.legendMax)
```

Add fields near the top of the class:

```kotlin
    private lateinit var legendBar: View
    private lateinit var legendMin: android.widget.TextView
    private lateinit var legendMid: android.widget.TextView
    private lateinit var legendMax: android.widget.TextView
```

At the end of `showLayer(layer)` add `bindLegend(layer)` and implement:

```kotlin
    private fun bindLegend(layer: WeatherLayer) {
        val gd = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            layer.scaleColors
        ).apply { cornerRadius = 6f }
        legendBar.background = gd
        legendMin.text = "${layer.scaleMin}${layer.scaleUnit}"
        legendMid.text = "${(layer.scaleMin + layer.scaleMax) / 2}${layer.scaleUnit}"
        legendMax.text = "${layer.scaleMax}${layer.scaleUnit}"
    }
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run + verify the legend updates per layer**

Switch layers → the gradient bar + min/mid/max labels change (Temp: -20°C…40°C; Wind: 0…30 m/s; etc.).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt
git commit -m "feat(weathermap): color-scale legend per active layer"
```

---

## Task 10: Tap-to-query popup

**Files:**
- Modify: `app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt`

- [ ] **Step 1: Add a MapEventsOverlay that queries OWM on single tap**

In `onCreate`, before `showLayer(activeLayer)`, add the events overlay (must be added first so it's below other overlays but still receives taps):

```kotlin
        val events = org.osmdroid.views.overlay.MapEventsOverlay(
            object : org.osmdroid.events.MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    queryPoint(p); return true
                }
                override fun longPressHelper(p: GeoPoint): Boolean = false
            }
        )
        map.overlays.add(0, events)
```

Add fields + the query method:

```kotlin
    private val pointRepo = com.nimboweather.forecast.data.weathermap.point.PointRepository()
    private var queryMarker: Marker? = null

    private fun queryPoint(p: GeoPoint) {
        androidx.lifecycle.lifecycleScope.launch {
            val fallback = String.format(java.util.Locale.US, "%.2f, %.2f", p.latitude, p.longitude)
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
```

Add imports at the top:

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run + verify tap query**

Tap anywhere on the map → a marker + info window appears showing the place name and the active layer's value (e.g. "Jakarta / 27°C"). Switch to Wind, tap again → value shows m/s.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nimboweather/forecast/ui/radar/RadarActivity.kt
git commit -m "feat(weathermap): tap-to-query point popup via OWM current weather"
```

---

## Task 11: Delete dead RainViewer code + final verification

**Files:**
- Delete: `app/src/main/java/com/nimboweather/forecast/data/radar/RainViewerApi.kt`,
  `RainViewerModels.kt`, `RainViewerTiles.kt`
- Delete: `app/src/test/java/com/nimboweather/forecast/data/radar/RainViewerTilesTest.kt`

- [ ] **Step 1: Confirm no remaining references**

Run: `grep -rn "RainViewer\|RadarRepository\|RadarTimeline\|RadarFrame" app/src/main app/src/test`
Expected: no matches (Task 6 removed the activity usage). If any remain, fix before deleting.

- [ ] **Step 2: Delete the files**

```bash
git rm app/src/main/java/com/nimboweather/forecast/data/radar/RainViewerApi.kt \
       app/src/main/java/com/nimboweather/forecast/data/radar/RainViewerModels.kt \
       app/src/main/java/com/nimboweather/forecast/data/radar/RainViewerTiles.kt \
       app/src/test/java/com/nimboweather/forecast/data/radar/RainViewerTilesTest.kt
```

- [ ] **Step 3: Full build + all unit tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; all tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore(weathermap): remove dead RainViewer radar code"
```

---

## Self-Review Notes

- **Spec coverage:** layer switch (Task 8), 3 model layers (Tasks 2/3/6), radar coverage-gated entry (Tasks 4/8), tap query (Tasks 5/10), base-map swap (Tasks 1/6), legend (Task 9). All locked-scope items covered. Deferred (time axis, city labels) intentionally absent.
- **Type consistency:** `WeatherLayer` fields (`owmLayer`, `scaleMin/Max/Unit/Colors`, `labelRes`) used identically in Tasks 2/6/8/9. `WeatherTiles.owmUrl/esriUrl/nexradUrl`, `RadarCoverage.hasNexrad`, `PointForecast.from(resp, layer, fallbackPlace)`, `PointRepository.query(lat, lon, layer, fallbackPlace)` consistent across tasks.
- **Known v1 limitations (by design):** no forecast time axis; no city temperature labels; tap value is OWM current weather (consistent provider with the heatmap, but a model value, not a tile-pixel readout); radar real-data only in the contiguous US.
- **External prerequisites:** `OPENWEATHER_API_KEY` (already used by the app) and a free `ESRI_API_KEY` must be in `local.properties` for tiles to render on device. Unit tests need neither.
