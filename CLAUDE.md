# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Communication

- **Always communicate with the user in Chinese (中文).** Code, identifiers, and commit messages stay in English; all explanations, questions, and summaries are in Chinese.

## Project

Native Android (Kotlin, View system + ViewBinding) weather app.
- Package: `com.nimboweather.forecast` · minSdk 24 / target & compile 35
- JVM target 17 (Java + Kotlin)
- Weather data: OpenWeatherMap (free `/data/2.5/*` + `/geo/1.0/*`)
- Ads: pluggable mediation abstraction over Google Mobile Ads (TopOn/MAX adapter slots reserved)

## Commands

All Gradle commands use the wrapper from the repo root.

```bash
./gradlew assembleDebug                 # build debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                  # build + install onto the connected device/emulator
./gradlew lintDebug                     # Android lint (non-blocking in CI)
./gradlew test                          # all unit tests
./gradlew :app:testDebugUnitTest        # debug-variant unit tests only
# Single test class / method:
./gradlew :app:testDebugUnitTest --tests "com.nimboweather.forecast.data.MoonPhaseTest"
./gradlew :app:testDebugUnitTest --tests "com.nimboweather.forecast.data.AirQualityIndexTest.usAqi_pm25_categoryBoundaries"
```

Unit tests live under `app/src/test/java/` (plain JUnit 4 — no Android instrumentation).

### Required local config (gitignored; not in repo)
- `local.properties` — copy `local.properties.example`; must set `sdk.dir` and `OPENWEATHER_API_KEY`. The key is read at config time in `app/build.gradle.kts` and injected as `BuildConfig.OPENWEATHER_API_KEY` (lines 10–14, 26). Missing it ⇒ empty string ⇒ all OWM calls 401.
- `app/google-services.json` — only required once Firebase RemoteConfig/FCM are actually wired. Package must match `com.nimboweather.forecast`.

### Toolchain quirks
- **JDK 17 required.** macOS users on JDK 25 will fail Gradle; pin JDK 17 (Spenser's machine already has this pinned globally; the wrapper does not auto-provision a JDK).
- **Non-ASCII path tolerated.** `gradle.properties` sets `android.overridePathCheck=true` because the repo lives at `…/塔酷 …` on Spenser's machine. Safe because there's no NDK. Don't remove this flag without also moving the repo.
- AdMob uses Google's **official test IDs** (`StrategyProvider.TestAdUnits` and the `APPLICATION_ID` in `AndroidManifest.xml`). Both must be swapped to real IDs before release.

## Architecture

### Process startup (`NimboApp.kt`)
On `Application.onCreate`:
1. `AdMediator.register(adapters, strategyProvider)` — **does not** initialize ad SDKs yet (must wait for UMP consent).
2. `AppOpenAdManager` registered as both an `ActivityLifecycleCallbacks` and a `ProcessLifecycleOwner` observer — shows app-open on cold start / foreground return.
3. `Notifications.ensureChannels` + `WeatherScheduler.schedule` (WorkManager periodic refresh).

The consent → SDK init handoff happens in the UI layer (`consent/` UMP flow → `AdMediator.initializeAds(context)`).

### Ads abstraction (`ads/` + `config/`)
The app **never** calls a network SDK directly. The shape:
- `AdMediator` (singleton) is the only entry point. It owns frequency capping (per-session count + cooldown), format selection, and the "render a Native ad as a full-screen interstitial" mix (`fullScreenNativeMixRatio`).
- `AdNetworkAdapter` is the per-network plug. Today only `adapters/AdmobAdapter` exists; TopOn/MAX adapters drop in by registering another instance — no call-site changes.
- `AdStrategy` (data) holds ad-unit IDs, enable/cap/cooldown per `AdFormat`, network preference, and the native-fullscreen mix ratio. Supplied by a `StrategyProvider`:
  - `LocalDefaultStrategyProvider` — compliant defaults with AdMob test IDs.
  - `RemoteConfigGateway` — production path; reads cached strategy now, swaps to live Firebase RemoteConfig once `google-services.json` is wired.

When adding a new ad surface or behavior, change `AdStrategy` + `AdMediator` — don't reach into adapters from UI.

### Data layer (`data/`)
- `RetrofitProvider` — single Retrofit/OkHttp instance with the kotlinx-serialization converter; logging interceptor enabled in debug.
- `WeatherApi` — Retrofit interface covering current weather, 5-day/3-hour forecast, air pollution, and geocoding (forward + reverse).
- `WeatherRepository` — thin wrapper; injects `BuildConfig.OPENWEATHER_API_KEY` so callers never see the key.
- `WeatherCache` — local persistence of last-known forecast per city (used by Widget/Notifications when offline).
- Pure-Kotlin helpers tested in unit tests: `AirQualityIndex` (US AQI from PM2.5 via EPA breakpoints) and `MoonPhase` (synodic-month phase + illumination). These are deliberately framework-free so they stay unit-testable.

### UI layer (`ui/`)
- `SplashActivity` (launcher) → `OnboardingActivity` (first run) → `MainActivity`.
- `MainActivity` + `MainViewModel` drive a `ViewPager2` of saved cities. Each page is a `WeatherPageFragment` with its own `CityWeatherViewModel`; a trailing `AddCityPageFragment` jumps to `FindCityActivity`.
- A left `DrawerLayout` holds the cities list, add-city, and settings (units, language).
- The home page is **built**, not hardcoded: `WeatherCardsBuilder` turns repository data into a `List<HomeCard>` (sealed hierarchy — Hero/Compass, Hourly, Daily, AQI, Moon, Sunrise/Sunset, generic InfoCard, grid section). `HomeCardRenderer` inflates the appropriate layout per card type and places them into the page's scroll view. `CardLayoutConfig` controls order and which cards form the 2-col grid.
- Visuals layered behind the cards:
  - `WeatherFxView` — scene system (clear-day, clear-night/stars, partly-cloudy, overcast, rain, thunder, snow, fog) chosen by OWM condition group + day/night flag from the icon's `d`/`n` suffix. Animations pause when the page isn't visible.
  - `SkyGradient` — per-condition gradient.
  - `CompassDialView` — the live compass hero (kept across the iOS-style redesign).
- `findcity/` — geocoded search + popular-city suggestions. `detail/ForecastDetailActivity` — full-screen drilldown for an hourly/daily entry. `ui/ads/NativeFullscreenActivity` — chrome for the native-as-fullscreen path used by `AdMediator`.

### Retention surfaces
- `work/WeatherScheduler` + WorkManager — periodic background refresh that updates the cache, optionally posts notifications via `notify/`, and triggers `widget/WeatherWidgetProvider` to redraw.
- `widget/` — home-screen widget. One size-responsive `WeatherWidgetProvider` renders small/medium/large layouts (`chooseLayout` by measured cell); falls back to `WeatherCache` when offline. Each layout is a `FrameLayout` with a dedicated `@id/widgetBg` scene `ImageView` whose alpha is driven per-widget by `WidgetAppearance.bgOpacity` via `RemoteViews.setImageAlpha` — **0 = transparent (wallpaper shows), 255 = solid scene art** (default `1f`, so unconfigured widgets look unchanged). `WidgetConfigActivity` picks the city and an opacity (`SeekBar`); `WidgetPrefs` persists both per `appWidgetId`. `WidgetStyle` + `WidgetAppearance` are pure/unit-tested (`bgOpacity`, theme, accent); see `docs/widget-matrix-spec.md` for the planned style-variety roadmap.

### Locales
Per-app language uses AppCompat per-app locales, persisted on pre-Android 13 via `AppLocalesMetadataHolderService` (declared in the manifest). Resource set: default (en) + `values-zh`.

## Docs

`docs/` contains the iOS-style home redesign spec (`home-redesign-spec.md`) plus phased plans (`phase1-toolbar`, `phase2-cards`) and a compass redesign spec. Read these before making non-trivial home-screen changes — recent commits implement those phases.

## CI

`.github/workflows/android-ci.yml` runs on push/PR to `main`: JDK 17 setup, installs `platforms;android-35` + `build-tools;35.0.0`, injects `OPENWEATHER_API_KEY` and (if available) `GOOGLE_SERVICES_JSON` from secrets, then `./gradlew assembleDebug` and uploads the APK. `lintDebug` runs but does not fail the build.
