# Retention — Spec (2026-06-23)

What capabilities an **ad-monetized Android weather app** needs to lift retention,
and how each maps onto NimboWeather's existing architecture.

> Source basis: deep-research run 2026-06-23 — 5 search angles → 18 sources → 81 claims →
> 25 adversarially verified (3-vote, need 2/3 to confirm) → **10 confirmed, 15 refuted**.
> **Read the evidence-quality note before trusting any number.** Most "push notifications
> lift retention 120–190%" vendor benchmarks were *refuted* (0-3). Only the mechanism
> survives, not the magnitude.

## Evidence quality (read first)

| Tier | What it covers | Trust |
|---|---|---|
| **Primary (1 source)** | Google: widget users +25% retention | ✅ use as planning anchor |
| **Official policy/docs** | AdMob interstitial placement rules; Firebase ad-frequency tuning | ✅ hard constraints |
| **Refuted (15 claims)** | All push-notification lift multiples (190%/3X/120%/2.6X/191%); category D30 "2.6%/3.4%"; interstitial "1/hour", "1–3/session"; "6–7% churn per bad ad" | ❌ do **not** plan against these — vendor marketing |
| **Mechanism-only** | Personalized+localized notifications work; quality ads don't *necessarily* churn | ⚠️ direction valid, magnitude unknown — A/B it yourself |

Net: build the *mechanisms* below; treat every operational number as an F2P heuristic to
validate by A/B + Remote Config, not a fact.

---

## Goals

1. **Make the widget a first-class retention surface** — the only primary-backed lever.
2. **Audit ad timing** so ads never violate AdMob placement rules or burn the first session.
3. **Ship habit/trust notifications** (rain nowcast + morning brief), opt-in, low-frequency.
4. **Co-tune monetization vs retention** via Remote Config, with a premium ad-free path.
5. **Differentiate** beyond the OS's built-in weather so the daily-check habit attaches to *us*.

## Non-goals (this iteration)
- **Chasing vendor lift numbers.** No KPI in this spec inherits a refuted benchmark.
- **Subscription product build-out.** Premium is reserved as an ad-removal SKU + rewarded
  gateway only; full IAP catalog is out of scope.
- **Replacing existing surfaces.** Everything here augments the current home/cards/ads stack.

---

## The five capabilities

### 1. Widget as a retention surface — **P0** ✅ (3-0, primary)
The single strongest, best-evidenced lever: Google's own data shows **+25% retention for
widget users** (developer.android.com). Passive home-screen presence pulls users back
*without* an app open.

- **Status:** `widget/WeatherWidgetProvider` + `WidgetConfigActivity` already ship.
- **Invest in:** multiple sizes, information density (now + hi/lo + condition + AQI/precip
  hint), refresh timeliness, and a smooth per-widget city-pick. Tie redraw into the existing
  `work/WeatherScheduler` cycle.
- **Why P0:** highest-certainty ROI in the whole report; we already own the surface.

### 2. Ad timing audit — **P0** ✅ (3-0, AdMob policy)
- **Hard rules (AdMob policy):** no interstitial on load, on exit, mid-task, or during
  critical flows — violations are penalized.
- **Mechanism evidence:** avoiding first-session ads lifted D1 +5–8%; keeping interstitials
  under 3/session retained more users (AdReact). Magnitudes are heuristic; the *direction*
  is robust.
- **Map to `ads/AdMediator` + `config/`:**
  - Confirm **no interstitial in the first session** and none at launch (App-Open is a
    separate, allowed format — keep it distinct).
  - Verify the **native-as-fullscreen mix** (`fullScreenNativeMixRatio`) only fires at
    natural transition points (e.g. returning from `detail/ForecastDetailActivity`), never
    mid-scroll.
  - Frequency cap already exists — keep it, but drive thresholds from Remote Config (see §5).

### 3. Habit & trust notifications — **P1** ⚠️ (3-0 mechanism, magnitudes refuted)
Effective recipe = **opt-in + location + stated preferences** → personalized, localized
alerts. Treat notifications as a *trust* surface, not a growth lever.

- **Build:** rain nowcast push (see `precip-nowcast-spec.md` — ~80% of its value is the push
  at the decision moment) + optional morning brief. One notification per rain event, quiet
  hours 22:00–07:00, strict opt-in.
- **Accuracy gates value:** nowcast precision depends on Open-Meteo `minutely_15` (per the
  data-source verdict in `precip-nowcast-spec.md`); a wrong alert costs trust, not builds it.
- **Do NOT** set KPIs from the refuted "190%/3X" push benchmarks.

### 4. Co-tuned monetization ↔ retention — **P1** ✅ (3-0)
- **Principle:** revenue up + retention down = trading away LTV, not winning. Start near
  ~1 ad/session and tune up against retention.
- **Premium = ad-free** (`block_ads_for_premium` flag) — paid users see zero ads.
- **Rewarded ads = opt-in IAP gateway** — users who engage rewarded ads spend *more* on IAP.
- **Map to `config/RemoteConfigGateway`:** drive cap/cooldown/format/mix-ratio from Firebase
  Remote Config so we A/B without shipping. This is exactly what the gateway was reserved
  for — needs `google-services.json` wired.

### 5. Differentiation beyond OS weather — **P2** ✅ (3-0)
Daily-check habit is real (53–60% check weather daily), but the OS weather app + ~45%
multi-app installs are the real competitors. The habit only retains *us* if we offer what
the built-in app can't.

- **Status:** AQI card, moon-phase card, and the `WeatherFxView` scene system already
  differentiate. Keep deepening distinctive, glanceable content rather than parity features.

---

## Priority summary

| Priority | Action | Evidence strength | Touches |
|---|---|---|---|
| **P0** | Widget upgrade (sizes / density / refresh timeliness) | Only primary, +25% | `widget/`, `work/WeatherScheduler` |
| **P0** | Ad-timing audit (no first-session interstitial; native-mix at transitions) | AdMob policy + D1 +5–8% | `ads/AdMediator`, `config/` |
| **P1** | Rain nowcast push, opt-in, low-frequency | Mechanism confirmed | `notify/`, `precip-nowcast-spec.md` |
| **P1** | Remote-Config ad frequency + premium ad-free + rewarded gateway | 3-0 | `config/RemoteConfigGateway` |
| **P2** | Deepen differentiated content (AQI/moon/FX) | Category competition | `ui/` cards |

**Read of NimboWeather:** the architecture already covers most of this —
`WeatherWidgetProvider`, `AdMediator` frequency cap, `RemoteConfigGateway`, `WeatherCache`
offline, and the AQI/moon/FX differentiation. The conclusion is **make the existing pieces
deeper and correctly tuned**, not fill gaps.

---

## Verification footnote

- **Confirmed (used above):** habit≠retention/differentiation needed (3-0); widget +25%
  (2-1, Google primary); notification mechanism via opt-in+location+prefs (3-0); interstitial
  placement rules + first-session avoidance (3-0); co-tune + premium ad-free + rewarded
  gateway (3-0).
- **Refuted (excluded):** every push-lift multiple from Urban Airship / Braze / MoEngage /
  Airship marketing PDFs (0-3); category D30 "2.6%/3.4%" (0-3 / 1-2); interstitial "1/hour"
  and "1–3/session" caps (0-3); "6–7% churn per bad ad" (1-2); "72% tolerate quality ads"
  (0-3).
- **Caveat:** 17 of 18 sources are vendor blogs; only the Google widget figure is primary.
  Validate all operational numbers with your own A/B before committing.
