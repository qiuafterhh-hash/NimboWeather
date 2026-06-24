# Changelog

All notable changes to NimboWeather are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/), versioning follows [SemVer](https://semver.org/).

## [Unreleased]
### Added
- Precipitation nowcast card + rain push notification; experimental weather radar (RainViewer tiles over OSM).
- Cloud test system: unit (JUnit) + UI instrumented (Espresso) + black-box (Maestro) + stability (monkey), gated as required PR checks on `main`.
- Version management: versionName/versionCode derived from git tags; tag-triggered Release workflow that builds the APK and publishes a GitHub Release with auto-generated notes.

### Changed
- App-Open ad no longer auto-shows in debug builds (test determinism); release behaviour unchanged.

## [0.1.0] - 2026-06-17
### Added
- Initial NimboWeather: multi-city current/hourly/daily forecast (OpenWeatherMap), radial compass dial hero, dynamic animated weather background, glassmorphism cards, AQI + moon phase, home widget, ongoing/alert notifications, onboarding, EN/中文 locales.
- Pluggable ad mediation (AdMob test units now; TopOn/MAX-ready abstraction) with server-configurable strategy.

<!--
Tagging a release:  see VERSIONING.md
[Unreleased]: changes on main not yet tagged.
-->
