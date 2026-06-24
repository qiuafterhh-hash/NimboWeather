# Precipitation Nowcast — Spec (2026-06-23)

A short-term rain forecast ("rain starts in ~15 min") surfaced as a home card **and**,
more importantly, as a **timely push notification**. Replaces the vague "heatmap" idea:
the radar map is the *display layer*, the notification is the *retention engine*.

> Reframe from the brainstorm: this is **not** a "heatmap feature". It is
> **precipitation nowcasting + smart push**. ~80% of the retention value is in the
> notification fired at the decision moment ("should I bring an umbrella?"); the
> animated radar map is a nice-to-have on top.

## Goals
1. **Nowcast card (L1)** — a home card that says, in plain language, whether rain is
   starting/stopping within the next hour, with a 15-min-resolution precipitation curve.
2. **Smart push (the retention engine)** — notify when the user's **current location**
   transitions dry→rain (or rain→stop) within the next hour. One notification per rain
   event, quiet hours 22:00–07:00.
3. **Radar map (L2, later)** — tap the card / the existing 🗺️ toolbar entry to expand an
   animated past-radar map. WiFi-preferred. Lowest priority.

## Non-goals (this iteration)
- **Per-minute precision.** Free data is 15-min resolution. Copy must not fake
  minute-level accuracy ("rain in 18 min" ❌). L2 future-extrapolation radar is paid — skip.
- **OWM as the nowcast source.** One Call 3.0 `minutely` requires a separate paid
  subscription — the free `/data/2.5` key returns 401 (verified 2026-06-23, all test cities).
- **Replacing the existing forecast.** Nowcast augments; it does not touch the 5-day/3-hour cards.

## Data sources (verified 2026-06-23)
| Source | Verdict | Use |
|---|---|---|
| OWM One Call 3.0 `minutely` | ❌ paid-only, 401 on free key | — |
| RainViewer free | ⚠️ past/current radar only, **no** future frames | L2 map animation (free, global) |
| **Open-Meteo** `minutely_15` | ✅ 15-min precip, global incl. emerging markets, no key | **L1 nowcast engine** |

**Verification evidence:** Open-Meteo returned 8/8 valid series for Jakarta, Manila, Lagos,
Mumbai, São Paulo, Bangkok, Cairo, Lahore — e.g. Jakarta `[0.40,0.20,0.20,0.20,0.20,0,0,0]`
mm (clearing), Mumbai `[0.20,0.20,0.20,0.10,0.10,0.10,0.10,0.20]` (steady drizzle).

### ⚠️ Licensing decision before launch (not a blocker for dev)
Open-Meteo's free tier is **non-commercial only**. This app is ad-monetized = commercial.
Before release, pick one: paid API (~€29/mo, <10k calls/day) **or** self-host (open source).
Free tier is fine to build/test against now.

## Architecture impact
Introduces a **second data provider** alongside OWM. Current
`RetrofitProvider` / `WeatherApi` / `WeatherRepository` are OWM-specific; Open-Meteo's
JSON shape differs entirely, so it needs its own Retrofit interface + repository.
Keep the **pure nowcast logic framework-free and unit-tested**, in the style of
`AirQualityIndex` / `MoonPhase` (`app/src/test/java/`).

- `data/Nowcast.kt` — **pure logic** (the testable core, **done**): reduces a 15-min precip
  series to a `NowcastState` (see state machine). Lives flat in `data/` next to its siblings
  `MoonPhase` / `AirQualityIndex`.
- `data/nowcast/OpenMeteoApi` — Retrofit interface, `minutely_15=precipitation`.
- `data/nowcast/NowcastRepository` — fetch + map the response into the `precipMm` list `Nowcast` consumes.
- `ui/.../HomeCard.Nowcast` + renderer — slots into the existing `WeatherCardsBuilder` /
  `HomeCardRenderer` sealed-card system; `CardLayoutConfig` places it directly under the Hero.
- `work/WeatherScheduler` — add a nowcast check step that may post a notification via `notify/`.

## L1 nowcast state machine (the unit-testable core)
Inputs: `precip[]` (mm, 15-min steps, ~4–8 steps = next 1–2 h), threshold `T = 0.1 mm`.
A step counts as rain when `value >= T`.

**4 fact-only states** (implemented in `data/Nowcast.kt`). "Starting soon vs later" is *not*
a separate state — it's a copy concern derived from `minutesUntilStart`, so the state machine
stays minimal and the wording lives in `Nowcast.headline()`.

| State | Condition | minutes |
|---|---|---|
| `Dry` | no step ≥ T | — |
| `RainStarting(minutesUntilStart)` | step 0 < T, first step ≥ T at index `i` | `i × 15` |
| `RainStopping(minutesUntilStop)` | step 0 ≥ T, first step < T at index `i` | `i × 15` |
| `RainingThroughout` | step 0 ≥ T, every step ≥ T | — |

`headline(state)` derives the 5 user-facing messages:

| State | Card copy (en) |
|---|---|
| `Dry` | "No rain in the next hour ☀️" |
| `RainStarting`, ≤ 15 min | "Rain starting within 15 min 🌧️" |
| `RainStarting`, later | "Rain expected in about {N} min 🌧️" |
| `RainStopping` | "Rain easing — should stop within {N} min 🌤️" |
| `RainingThroughout` | "Rain continuing for the next hour 🌧️" |

Minutes are honest multiples of the 15-min grain (15/30/45/60), never a false-precise minute.
Below the headline: a 15-min-resolution precipitation bar curve (pure Canvas, no map tiles,
no extra traffic — safe for low-end / data-sensitive emerging-market devices).

## Push notification (retention engine)
- **Trigger:** in `WeatherScheduler`'s periodic run, compute nowcast for the **current
  location**. Fire when transitioning **Dry → RainStarting*** (and optionally Rain → stop).
- **Dedup:** at most one notification per rain event (track last-notified event state).
- **Quiet hours:** suppress 22:00–07:00 local.
- **Copy:** "☔️ Rain likely near you within the next ~30 min — grab an umbrella."

## Open product decisions
1. **Location:** current GPS location vs saved cities. → **GPS-first** (nowcast is about
   "where I'm standing"). Permission requested **lazily** on first card tap, framed around
   the rain-alert value — not at cold start.
2. **Graceful degradation:** if Open-Meteo `minutely_15` is unavailable for a point, fall
   back to "rain possible in the next hour" from the existing 3-hour forecast.

## Riskiest assumption (cannot be tested at design time)
> "An accurate rain push notification actually brings users back."

Validate post-launch on a small cohort: nowcast-push cohort vs control, measure D1/D7
retention + notification CTR. Do **not** over-build L2 before this is proven.

## Build order
1. ✅ `Nowcast.kt` pure logic + unit tests (`NowcastTest`, 14 tests).
2. ✅ Open-Meteo data layer — `data/nowcast/` (models, `OpenMeteoMapper` + 4 tests, Retrofit, repo).
3. ✅ `HomeCard.Nowcast` card + `NowcastCurveView` (Canvas), wired through builder + per-city VM.
4. ✅ Push trigger in `WeatherWorker` — `NowcastAlerts.decide()` (10 tests, dedup + quiet hours),
   `nowcastEventKey` persisted in `AppPrefs`, posted via `Notifications.postNowcast` (new channel).
5. 🟡 **L2 radar — built, but NOT recommended for this app (see findings).** `RadarActivity`
   (osmdroid + RainViewer overlay) behind the 🗺️ toolbar entry; data layer `data/radar/`
   (`RainViewerTiles` + 5 tests). Chrome, base map, animation controls, city marker all verified
   on device. The radar precipitation overlay does **not** render yet (open bug). More importantly,
   device testing proved L2 is the wrong investment for the target markets — see below.

### L2 device-test findings (2026-06-23) — why radar is a poor fit here
1. **RainViewer has ZERO radar coverage over the target emerging markets.** Proven at tile level:
   a Jakarta radar tile is **334 bytes (empty)**, identical to an open-ocean tile; an Amsterdam
   tile is **15 KB (full data)**. RainViewer aggregates *ground radar*, and Indonesia / much of
   Africa / South Asia have little or none. Open-Meteo (model-based, global) covers these markets;
   RainViewer (radar-based) does not. **L2 radar would be blank for most of this app's users.**
2. **OpenStreetMap tile servers block embedded apps** — `SSLHandshakeException: connection closed`
   on every `tile.openstreetmap.org` request. Switched base map to **Carto dark** (works). At
   matrix scale even Carto's free tier will rate-limit; a keyed provider (Mapbox/MapTiler) is
   required before launch.
3. **Open bug:** the RainViewer `TilesOverlay` does not render precipitation despite valid 15 KB
   tiles and a working base map. Suspected osmdroid provider/overlay wiring; not yet root-caused.

> Conclusion: keep L2 **gated**. Build/finish it **only if** L1 push moves D1/D7 retention AND the
> target markets gain radar coverage (or a model-based map-tile source replaces RainViewer). The
> original gating was right; device testing added a second, stronger reason.

### Decisions made during build (deviations from the plan above)
- **Push location = active/selected city, not background GPS.** The existing `WeatherWorker`
  already runs on `CityStore.selected`; background location is a Play-Store red flag for the
  matrix. The "GPS-first" open decision is deferred — foreground card can request GPS lazily later.
- **State machine = 4 fact-only states**, "soon vs later" derived in `headline()` (see table above).
- **Notify on `RainStarting` only** (the dry→rain transition); `RainStopping` is computed but not
  pushed, to avoid notification fatigue. Quiet hours 22:00–06:59.

## How to verify the push on a device (no waiting for real rain)
The decision is unit-tested; to see an end-to-end notification, force the worker against a
location that currently has rain in its `minutely_15`, or temporarily lower the threshold and
point `CityStore.selected` at a wet city. The card (L1) is directly visible whenever the
selected city has Open-Meteo data.
