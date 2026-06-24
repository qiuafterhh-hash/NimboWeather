# Widget Upgrade — Implementation Plan (2026-06-23)

Concrete plan to turn the current minimal widget into a first-class retention surface,
per `retention-spec.md` §1 (the only **primary-backed** lever: Google measured **+25%
retention** for widget users).

> Scope = the P0 row in `retention-spec.md`: **sizes / info density / refresh timeliness**,
> plus visual polish so the widget matches the in-app iOS-style redesign.

---

## Current state (audited 2026-06-23)

| Piece | File | State |
|---|---|---|
| Provider | `widget/WeatherWidgetProvider.kt` | renders city + temp + condition only |
| Layout | `res/layout/widget_weather.xml` | one `LinearLayout`, 3 `TextView`s, flat `widget_bg` |
| Config | `widget/WidgetConfigActivity.kt` | per-widget city pick ✅ |
| Info xml | `res/xml/weather_widget_info.xml` | 110×110 min, resizable, `updatePeriodMillis=3600000` |
| Data | `WeatherSnapshot` (`data/WeatherCache.kt`) | `city, temp, symbol, condition, icon` |
| Per-widget refresh | `work/WidgetRefreshWorker.kt` | fetches one city, writes `WidgetPrefs.setSnapshot` |
| Periodic refresh | `work/WeatherWorker.kt` | updates **global `WeatherCache`** + calls `refresh()` |

### Gaps this plan fixes
1. **Thin content** — no hi/lo, feels-like, condition glyph, AQI, or "updated" time.
2. **One layout for all sizes** — a 4×2 widget shows the same as a 2×1; wasted space.
3. **Flat visuals** — static `widget_bg`, no condition-driven color (the app already has
   `SkyGradient` + day/night logic; the widget ignores it).
4. **Refresh staleness (real bug)** — `WeatherWorker` periodic refresh updates only the
   global cache and re-renders; **per-widget configured cities are NOT re-fetched**, so a
   widget tracking "Tokyo" while the app views "London" only updates on the system's
   ~hourly `onUpdate`. No manual refresh, no stale indicator.

---

## Design

### Three responsive layouts (one provider, size-switched)
Use `AppWidgetManager.getAppWidgetOptions` width/height buckets to pick a `RemoteViews` at
render time (single provider, no extra receivers). Targets:

| Size | Cells | Shows |
|---|---|---|
| **Small** `widget_weather_small.xml` | 2×1 | icon · temp · city |
| **Medium** `widget_weather_medium.xml` | 4×2 | + hi/lo · feels-like · condition text · updated-time |
| **Large** `widget_weather_large.xml` | 4×3 | + AQI chip · 3-hour mini strip (next 3 forecast slots) |

`weather_widget_info.xml`: set `minWidth≈150dp`, `minHeight≈70dp`, add
`android:targetCellWidth/Height` (API 31+), `android:description`, and a
`previewLayout`/`previewImage`. Keep `resizeMode="horizontal|vertical"`.

### Richer snapshot (backward-compatible)
Extend `WeatherSnapshot` with **nullable, defaulted** fields — safe because prefs JSON uses
`ignoreUnknownKeys` and old blobs simply deserialize the new fields as null:

```kotlin
@Serializable
data class WeatherSnapshot(
    val city: String,
    val temp: Int,
    val symbol: String,
    val condition: String,
    val icon: String?,
    val hi: Int? = null,         // daily high  (from forecast aggregation)
    val lo: Int? = null,         // daily low
    val feelsLike: Int? = null,  // CurrentWeather.main.feelsLike
    val aqi: Int? = null,        // US AQI via AirQualityIndex (large widget only)
    val updatedAt: Long? = null  // epoch millis of fetch, for "updated 5m ago" + staleness
)
```

- **hi/lo**: aggregate today's slots from `WeatherRepository.forecast()` (min/max of
  `main.temp` for matching date). Avoids relying on the unreliable current-endpoint
  `temp_min/max`. One extra call, already used elsewhere.
- **aqi**: `WeatherRepository.airPollution()` → `AirComponents.pm25` → existing
  `AirQualityIndex` helper. **Large widget only**, gated to avoid quota burn.
- **feelsLike**: free, already in `CurrentWeather.main.feelsLike`.

### Visuals
- **Background**: reuse `SkyGradient.colorsFor(icon)` to bake a gradient. `RemoteViews`
  can't host a live `GradientDrawable`, so render the gradient to a `Bitmap` and
  `setImageViewBitmap` on a background `ImageView`, **or** ship a small set of static
  `widget_bg_{clear,clouds,rain,storm,snow,mist}_{day,night}.xml` gradient drawables and
  `setInt(R.id.bg, "setBackgroundResource", resId)`. Prefer the static-drawable set (cheaper,
  no bitmap alloc on every redraw). Day/night from icon `d`/`n` suffix, same rule as
  `SkyGradient`.
- **Condition glyph**: map OWM icon code → a local vector (we only have generic
  `ic_cloud`; add a small weather-glyph set, or `setImageViewBitmap` from the OWM CDN via
  `IconUrls.owm(icon)` decoded in the worker and cached). Recommend a **local vector set**
  to stay offline-correct (emerging-market weak-network goal in `retention-spec.md`).

### Refresh timeliness (fixes the bug)
1. **Hook per-widget refresh into the periodic cycle.** In `WeatherScheduler` /
   `WeatherWorker`, after the global refresh, enqueue `WidgetRefreshWorker` for **every**
   live widget id (`AppWidgetManager.getAppWidgetIds(...)`), so configured cities update on
   the same WorkManager cadence — not just the system's hourly `onUpdate`.
2. **Manual refresh.** Add a small refresh icon on medium/large; `setOnClickPendingIntent`
   → a broadcast to the provider that enqueues `WidgetRefreshWorker`. Shows immediate feedback.
3. **Staleness indicator.** Using `updatedAt`, render "updated 5m ago"; if older than e.g.
   2× the refresh period, dim + show a stale dot so users trust what they see.
4. Keep `updatePeriodMillis` as the floor; WorkManager is the real driver (more reliable,
   network-constrained).

---

## Phasing

> **Status (2026-06-23):** Phases A, B, C **done** + the large-widget 3-hour strip **done**.
> 23 new unit tests (63 total, all green); `assembleDebug` + `installDebug` pass; app runs
> on device (Samsung SM-A546E / Android 15) with no crashes or RemoteViews errors. Live
> visual render of the three sizes still needs manual placement (adb can't drag a widget
> onto the launcher). Phase D (polish) not started.

### Phase A — content + data (no UX risk) — ✅ DONE
- ✅ `WeatherSnapshot` extended with nullable `hi/lo/feelsLike/aqi/updatedAt` (+ `hours`);
  `WeatherCache` save/load persists them via sentinels; `WidgetPrefs` carries them as JSON.
- ✅ `WidgetRefreshWorker` builds the rich snapshot via new `data/SnapshotBuilder` (current
  + forecast + optional AQI, best-effort); `WeatherWorker` enriches its global snapshot
  inline (hi/lo + feelsLike + updatedAt) without a duplicate `current()` call.
- ✅ `data/DailyHiLo` (pure, window-based, timezone-safe) — **6 unit tests**.

### Phase B — responsive layouts + visuals — ✅ DONE
- ✅ `widget_weather_{small,medium,large}.xml` + 8 condition gradient backgrounds
  (mirroring `SkyGradient`) + 6 local weather glyph vectors (offline-correct).
- ✅ `WeatherWidgetProvider.render` reads `getAppWidgetOptions`, size-buckets the layout,
  binds all fields, sets condition background + glyph. `onAppWidgetOptionsChanged`
  re-renders on resize.
- ✅ `widget/WidgetVisuals` (icon → scene → bg/glyph) — **4 unit tests**.
- ✅ `weather_widget_info.xml` updated (sizes, `previewLayout`, `targetCell*`,
  `description` string in en + zh). Dead `widget_weather.xml` / `widget_bg.xml` removed.

### Phase C — refresh timeliness + AQI — ✅ DONE
- ✅ **Bug fix:** `WeatherWorker` now calls `WeatherWidgetProvider.enqueueRefreshAll`, so
  every configured widget re-fetches its own city on the WorkManager cadence — not just the
  system's hourly `onUpdate`.
- ✅ Manual-refresh tap (explicit broadcast `ACTION_REFRESH` → `WidgetRefreshWorker`) on the
  large widget; `widget/WidgetFormat` "updated Xm ago" + stale amber state — **7 unit tests**.
- ✅ AQI on the large widget (gated `includeAqi` extra call), rendered as a chip.

### Large-widget 3-hour strip — ✅ DONE
- ✅ `data/ForecastSlots` (pure "next N slots") — **6 unit tests**; `HourSlot` added to the
  snapshot; `SnapshotBuilder` builds 3 pre-formatted slots from the forecast it already
  fetches (no extra API call). Strip hides when no hourly data.

### Phase D — polish — ⏳ NOT STARTED
- Config screen preview of the chosen size, theme (light/dark/auto) toggle persisted in
  `WidgetPrefs`, finer tap-targets.
- (Deferred earlier, now folded in) nothing else outstanding from A–C.

---

## Files touched (actual)

| Action | Path |
|---|---|
| edit | `data/WeatherCache.kt` (snapshot fields + sentinels + `HourSlot`) |
| new | `data/SnapshotBuilder.kt` (shared rich-snapshot builder) |
| new | `data/DailyHiLo.kt` (pure forecast→hi/lo) + `DailyHiLoTest` |
| new | `data/ForecastSlots.kt` (pure next-N slots) + `ForecastSlotsTest` |
| edit | `work/WidgetRefreshWorker.kt` (uses `SnapshotBuilder`, AQI on) |
| edit | `work/WeatherWorker.kt` (inline hi/lo+feels+updatedAt; `enqueueRefreshAll`) |
| edit | `widget/WeatherWidgetProvider.kt` (size-switch render, strip, refresh broadcast) |
| new | `widget/WidgetVisuals.kt` (+ `WidgetVisualsTest`) |
| new | `widget/WidgetFormat.kt` (+ `WidgetFormatTest`) |
| new | `res/layout/widget_weather_{small,medium,large}.xml` |
| new | `res/drawable/widget_bg_{clear,clouds}_{day,night}.xml`, `widget_bg_{rain,storm,snow,mist}.xml` |
| new | `res/drawable/ic_wx_{sun,partly_day,rain,snow,storm,fog}.xml`, `ic_refresh.xml`, `widget_chip_bg.xml` |
| edit | `res/xml/weather_widget_info.xml` + `widget_description` string (en/zh) |
| del | `res/layout/widget_weather.xml`, `res/drawable/widget_bg.xml` (dead) |
| — | `work/WeatherScheduler.kt` **unchanged** — the per-widget hook lives in `WeatherWorker` |

## Non-goals
- Interactive/list widgets (`RemoteViewsService`) — dissent in the research flagged
  interactive widgets as *not* the +25% driver; passive glanceable is the goal.
- Putting ads in the widget — out of scope and against AdMob widget policy.
- New API surface beyond existing `forecast` / `airPollution`; AQI stays gated to large only.

## Verification

**Done (automated):**
- 23 new unit tests across `DailyHiLoTest` (6), `WidgetVisualsTest` (4), `WidgetFormatTest`
  (7), `ForecastSlotsTest` (6) — 63 total in the suite, 0 failures (clean `--rerun-tasks`).
- `./gradlew :app:testDebugUnitTest`, `:app:assembleDebug`, `installDebug` all pass.
- App launches and runs on device (Samsung SM-A546E / Android 15); no FATAL / InflateException
  / RemoteViews errors in logcat.
- Lint could not run in the dev environment (blocked fetching lint artifacts; non-blocking in CI).

**Still manual (needs a human on the launcher):**
- Place the widget, resize across small/medium/large, confirm layout swap + strip/AQI at large.
- Tap ↻ on large → re-fetch + "Updated just now".
- Toggle airplane mode (offline → last snapshot + amber stale label after 3h).
- Change tracked city in config; let a periodic refresh run and confirm a non-active-city
  widget updates (the bug fix).
