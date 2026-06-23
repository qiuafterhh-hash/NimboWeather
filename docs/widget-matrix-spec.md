# Widget Matrix Spec — scaling Nimbo's home-screen widgets

> Source: teardown of competitor **Local Weather Alerts – Widget 1.7.2**
> (`live.weather.vitality.studio.forecast.widget`, AccuWeather data, AdMob + Billing).
> This doc captures what that app does well in its widget subsystem and turns it into a
> concrete, incremental plan for Nimbo's existing `widget/` package.

---

## 1. What the competitor ships (decoded from the APK)

**12 widget providers**, each a separate `<receiver>` + `appwidget-provider` XML + `RemoteViews` layout.
They are NOT 12 sizes — they are **distinct visual STYLES**, each offered in a fixed size bucket:

| Provider | Style family | XML (size) | minW × minH | Notes |
|---|---|---|---|---|
| AppWidgetProvider1 | Classic | classic_size21 | 110×40 | 2×1, lock-screen + home |
| AppWidgetProvider3 | Classic | classic_size41 | 250×40 | 4×1 |
| AppWidgetProvider2 | Classic | classic_size42 | 250×110 | 4×2, TextClock w/ timezone |
| AppWidgetProvider8 | Normal | normal_size42 | 250×110 | 4×2 opaque |
| AppWidgetProvider6 | "Apollo" | apollo_size42 | 320×110 | 4×2 designer skin |
| AppWidgetProvider7 | Daily | widget_daily | — | multi-day forecast row |
| AppWidgetProvider4 | Daily (glass) | transparent_daily | — | transparent variant |
| AppWidgetProvider10/11/12 | Material | material_size10/11/12 | 140×110 | **previewLayout + targetCellWidth/Height** (A12 picker) |
| ForClockSenseWidget | "Sense" clock | sense | — | clock-forward skin |
| AppWidgetReceiver | Hourly chart | hourly_chart | 320×180 | drawn chart strip |

Key techniques worth stealing:

1. **Style = product surface.** Variety is a Play-Store growth lever ("12 beautiful widgets")
   and a retention hook (a widget on the home screen ≈ never uninstalled). Each style has its
   own `previewLayout` so the Android 12+ picker shows a real preview, not a grey box.

2. **Per-widget appearance, keyed by `appWidgetId`.** `ForAppWidgetConfig.ClassicWidgetConfig`
   stores a **background opacity float** and an **accent color int** in SharedPreferences under
   keys like `"<styleKey><appWidgetId>"`. Rendering then does:
   ```java
   remoteViews.setInt(R.id.widget_root, "setBackgroundColor",
       Color.argb((int)(opacity * 255f), 0, 0, 0));   // transparent → opaque black
   ```
   That single line is the entire "transparent widget" feature — a reflective setter on the
   root view's `setBackgroundColor`. Nimbo currently hard-codes a drawable background, so it
   has **no opacity control** today.

3. **Timezone-correct clock for free.** Classic/Sense use `TextClock` and push the city's
   IANA zone via `remoteViews.setString(R.id.tc_time, "setTimeZone", zoneName)` — the clock
   ticks without any worker waking up.

4. **Refresh throttle.** `WidgetUpdateWork` keeps an in-memory `Map<cityKey, lastFetchMillis>`
   and a **10-minute TTL** (`600000`). A refresh request that hits a fresh key re-renders from
   cache instead of calling the network. Request types are encoded (`0=current, 1=daily,
   2=both, 3=auto`) and dispatched in `doWork()`.

5. **Update fan-out.** System `onUpdate` (hourly, `updatePeriodMillis` 30 min on most) is only
   a backstop; the real cadence is WorkManager enqueuing `WidgetUpdateWork` per `appWidgetId`,
   plus the periodic app refresh worker calling an `updateAll` that loops every live id.

---

## 2. Where Nimbo already wins (don't regress these)

Nimbo's `WeatherWidgetProvider` is a **single size-responsive provider**, which is architecturally
better than 12 copy-pasted classes:

- `chooseLayout(options)` switches small/medium/large by `OPTION_APPWIDGET_MIN_WIDTH/HEIGHT` — one
  provider covers all sizes via resize, vs the competitor's fixed buckets.
- `WeatherCache` + `WidgetPrefs.getSnapshot(id)` give a real **offline fallback**; the competitor
  shows stale or blank when the city has no cached fetch.
- `WidgetFormat` (staleness/relative-time) and `WidgetVisuals` (scene mapping) are **pure and
  unit-tested**. Keep that discipline for anything added below.
- Persistence is kotlinx-serialization JSON per id, not ad-hoc keys.

So the move is **not** "copy the 12-provider shape." It's: keep the size-responsive core, and
**add a STYLE dimension and an APPEARANCE config on top of it.**

---

## 3. The plan — add Style × Appearance to Nimbo

### 3.1 Two new orthogonal axes

- **Style** (`WidgetStyle`): visual family — `CLASSIC`, `GLASS`, `DAILY`, `HOURLY`, `CLOCK`.
  Reuses the existing small/medium/large layouts where it can; new styles add their own layouts.
- **Appearance** (`WidgetAppearance`): per-widget `bgOpacity` (0..1), `theme` (AUTO/LIGHT/DARK),
  and `accent` color. Stored per `appWidgetId`, exactly like the competitor.

Both are just data — the render core stays one function.

### 3.2 New files (provided, R-free, unit-testable)

- `widget/WidgetStyle.kt` — the style enum + its layout/preview/size metadata.
- `widget/WidgetAppearance.kt` — appearance data + the `argb(opacity)` math and theme resolution.
  No `R` references, so it compiles standalone and is unit-testable like `WidgetFormat`.

### 3.3 Patch `WidgetPrefs` (add appearance, mirror the competitor's per-id keys)

```kotlin
// in prefs/WidgetPrefs.kt
fun setAppearance(widgetId: Int, a: WidgetAppearance) {
    sp.edit().putString("w_appr_$widgetId", json.encodeToString(a)).apply()
}
fun getAppearance(widgetId: Int): WidgetAppearance =
    sp.getString("w_appr_$widgetId", null)
        ?.let { runCatching { json.decodeFromString<WidgetAppearance>(it) }.getOrNull() }
        ?: WidgetAppearance()                       // sensible default
// remember to also remove "w_appr_$widgetId" in clear()
```

### 3.4 Apply opacity in `WeatherWidgetProvider.bind()`

Replace the hard drawable background with the competitor's reflective-setter approach so users
get a real transparency slider:

```kotlin
val appr = WidgetPrefs(context).getAppearance(id)
// keep the scene drawable as the "opaque" look…
views.setInt(R.id.widgetRoot, "setBackgroundResource", WidgetVisuals.background(s.icon))
// …then overlay a tunable scrim so 0 = fully transparent, 1 = solid
views.setInt(R.id.widgetScrim, "setBackgroundColor", appr.scrimArgb())
views.setInt(R.id.wTemp, "setTextColor", appr.textColor())
```

(Needs a full-bleed `@id/widgetScrim` View above the bg and below the content in each layout.)

### 3.5 Throttle refresh (port the 10-min TTL)

Nimbo's `enqueueRefresh` fires unconditionally. Add a per-widget guard so resize/onUpdate storms
don't spam OWM (and burn quota):

```kotlin
private val lastFetch = java.util.concurrent.ConcurrentHashMap<Int, Long>()
private const val REFRESH_TTL = 10 * 60_000L   // matches competitor's 600000

fun enqueueRefresh(context: Context, widgetId: Int, force: Boolean = false) {
    val now = System.currentTimeMillis()
    if (!force && now - (lastFetch[widgetId] ?: 0) < REFRESH_TTL) {
        render(context, AppWidgetManager.getInstance(context), widgetId)  // cache re-render
        return
    }
    lastFetch[widgetId] = now
    WorkManager.getInstance(context).enqueue(/* …existing… */)
}
```

### 3.6 Multiple Play-Store widgets without 12 classes

To list N widgets in the picker you DO need N `<receiver>` entries (the OS keys the picker off the
component). But they can be **thin subclasses sharing one render core** — not 12 copies:

```kotlin
abstract class StyledWidgetProvider(val style: WidgetStyle) : WeatherWidgetProvider()
class GlassWidgetProvider  : StyledWidgetProvider(WidgetStyle.GLASS)
class DailyWidgetProvider  : StyledWidgetProvider(WidgetStyle.DAILY)
class ClockWidgetProvider  : StyledWidgetProvider(WidgetStyle.CLOCK)
```

`render()` reads `style` (default `CLASSIC`) to pick the layout set; everything else is shared.
Each gets its own `appwidget-provider` XML with a distinct `previewLayout`, `description`, and
`targetCellWidth/Height` (Android 12 sizing). Ship 3–5 strong styles, not 12 mediocre ones.

### 3.7 Config screen: add a theme/opacity step

`WidgetConfigActivity` currently only picks a city. Add a second section (or a second page) with:
- an opacity **SeekBar** → `appr.copy(bgOpacity = progress/100f)`,
- a LIGHT/DARK/AUTO toggle,
- a small accent swatch row,
- a **live `RemoteViews` preview** re-rendered on every change (call `render()` into a host
  `AppWidgetHostView` or just an `ImageView` of the bound views).
Persist via `WidgetPrefs.setAppearance(widgetId, appr)` before `setResult(RESULT_OK)`.

---

## 4. Suggested rollout (small PRs)

1. **Land `WidgetStyle` + `WidgetAppearance`** (these two files) + unit tests for `scrimArgb`/theme.
2. **Opacity scrim** in the existing 3 layouts + `WidgetPrefs.appearance` + config SeekBar. Ship —
   "transparent weather widget" is a high-intent Play-Store search term.
3. **Refresh TTL** throttle (quota + battery hygiene; required before scaling widget count).
4. **GLASS + DAILY styles** as thin subclasses with their own preview XML. Now the listing can
   advertise multiple widgets.
5. Optional: **CLOCK** style using `TextClock` + per-city `setTimeZone` (zero-cost ticking clock).

## 5. Monetization tie-in (matrix context)

Widgets feed the same loop the competitor monetizes: home-screen presence → frequent app re-opens
→ `AppOpenAdManager` (already in `NimboApp`) shows app-open on foreground return. More installed
widgets ⇒ more foregrounds ⇒ more app-open impressions, with no extra ad surface. Keep the widget
itself **ad-free** (Play policy forbids ads in widgets) — it's a traffic source, not an ad slot.
