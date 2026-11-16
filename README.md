# SportsSuitability

Android app (Kotlin + Jetpack Compose) that answers one question for every place you save:
**can I go today?** — for **surf**, **windsurf** and **skiing**.

It does three things:

1. Shows every forecast reading that matters for the sport, now and for the next 7 days.
2. Turns those readings into a go / no-go verdict with a 0–100 score and the reasoning behind it.
3. Tells you about it without being opened — a Sunday-evening outlook for the week ahead and a
   go/no-go alert on the morning itself.

## Verdicts

Each spot and each day gets a `Verdict`: a suitability band, a score, and three lists of reasons.

| Band | Meaning |
| --- | --- |
| **Ideal** | Score ≥ 85 |
| **Bueno** | Score ≥ 70 |
| **Regular** | Score ≥ your configured minimum |
| **Flojo** | Below your configured minimum |
| **No ir** | A hard limit was crossed — the score is irrelevant |
| **Sin datos** | Nothing measurable was available |

Two kinds of rule produce it:

- **Hard limits** are deal-breakers. Cross one and the day is *No ir* no matter how good
  everything else looks — e.g. ski wind above 70 km/h. Each has its own on/off switch, so you
  can demote it to an ordinary scoring factor instead of deleting it.
- **Preferences** shape the score. Each available reading contributes a weighted 0–1 sub-score;
  readings that are missing are skipped rather than counted as zero.

Every reason is written out with the actual number and the limit it broke
("Viento 82 km/h (límite 70 km/h): remontes en riesgo de cierre"), so both the card and the
notification explain themselves.

### What you can configure

All of it lives in **Ajustes**, in metric units internally so switching display units never
changes a rule. Per sport there is also a *Restaurar* button.

**Surf** — ideal wave range, hard min/max wave, ideal and hard-minimum period, ideal and hard
maximum wind, hard minimum sea temperature, and whether offshore wind is rewarded.

**Windsurf** — ideal wind range, hard minimum *and* maximum wind (too little wind is its own
deal-breaker), maximum gust spread (gust minus average — how gusty the day is), hard maximum
wave, hard minimum air and sea temperature, and whether cross-shore wind is rewarded.

**Ski** — **espesor recomendado** (the depth that scores full marks) and a hard minimum depth
below it, fresh-snow target, ideal and hard-maximum wind, hard-maximum gusts, ideal temperature
range with hard limits either side, maximum rain, minimum visibility, and a maximum freezing
level (above the resort it rains instead of snowing).

**Global** — the minimum score at which the app says "sí puedes ir", the four unit choices, and
the alert schedule.

**Per spot** (the ⚙ icon on each card) — the **coast orientation**, which is what lets the app
tell offshore from onshore wind, and for ski resorts the **altitude** the forecast is taken at
(leave the geocoder's village altitude and you will see rain where the slopes get snow).

## Alerts

Both are self-renewing one-shot WorkManager jobs rather than periodic requests: periodic work
drifts and cannot be pinned to a wall-clock time, and "domingo por la tarde" has to actually
land on Sunday afternoon. Each worker re-arms the next run when it finishes, so the time stays
exact across DST changes and a settings change takes effect immediately.

- **Previsión semanal** — default Sunday 18:00, configurable to any weekday and time. One
  notification per sport covering **every** saved place, listing the days of the coming week
  worth going and how good each is. Today is excluded; the point is the week ahead.
- **Aviso del día** — default 07:00, configurable. Per spot: "✅ Hoy sí" or "🚫 Hoy no", with
  the reasons on expand. By default it reports no-go days too — knowing not to drive two hours
  is worth as much as knowing to go — and there is a switch to only hear the good news.

Alerts are on for every spot by default; the switch on each card opts one out. **Probar diario**
and **Probar semanal** in settings fire one immediately so you can see what it looks like.

## Data shown

Everything below is rendered as labelled chips, grouped with the section that matters most to
the sport first. Anything the model does not publish for a location is omitted rather than shown
blank. Tap **Ver todos los datos** on a card for the full set, or any day in the strip for that
day's full set plus an hour-by-hour breakdown.

- **Sea** — wave height, period and direction; swell height, period, peak period and direction;
  wind-wave height and period; sea surface temperature; ocean current speed and direction; sea
  level. Plus **tide turns** (high/low with time and height), derived from the hourly sea-level
  curve.
- **Wind** — speed, gusts, direction as a compass point, and whether it is offshore, cross-shore
  or onshore for that spot.
- **Snow** — snowpack depth, fresh snow now and today, freezing level, spot altitude.
- **Air** — sky description, temperature, apparent temperature, humidity, cloud cover,
  precipitation and probability, precipitation hours, pressure, visibility, UV index.
- **Sun** — sunrise, sunset, daylight hours, sunshine hours, peak UV.

The card also shows the **best window of the day**: the longest unbroken run of hours that
clears your rules.

## Weather provider

**[Open-Meteo](https://open-meteo.com/)**, free tier, no API key and no account.

- `geocoding-api.open-meteo.com` — place name → lat/lon/elevation.
- `api.open-meteo.com/v1/forecast` — atmosphere. Fetched for **every** sport, including surf:
  wind is what separates a clean wave from a mess. One request returns current, hourly and
  daily, so a spot costs one round trip instead of three.
- `marine-api.open-meteo.com/v1/marine` — waves, swell, sea temperature and sea level. Added
  for surf and windsurf only.

The two are fetched in parallel and fail independently: if the marine model is quiet the spot
still renders its weather with a warning, and vice versa.

Two caveats worth knowing. Snow depth is the **model's** snowpack estimate, not a resort report —
treat it as a good indicator, not a piste bulletin. And Open-Meteo has no tide table on the free
tier, so tide turns are local extrema of the hourly sea-level (MSL) series, which tracks the real
tide closely but is not an official prediction.

If you later want higher rate limits or commercial use, Open-Meteo sells an API-key plan — swap
the base URLs in [`NetworkModule.kt`](app/src/main/java/com/zenvairo/sportssuitability/data/remote/NetworkModule.kt)
for the `customer-*` endpoints and add the key as a query param.

## Architecture

- **UI** — Compose + Material 3, one `MainActivity` with a bottom bar per sport
  ([`ui/screens/SportScreen.kt`](app/src/main/java/com/zenvairo/sportssuitability/ui/screens/SportScreen.kt)),
  pull-to-refresh, expandable cards and a per-day detail dialog.
- **Rules engine** —
  [`data/repository/Suitability.kt`](app/src/main/java/com/zenvairo/sportssuitability/data/repository/Suitability.kt).
  Plain Kotlin with no Compose dependency (colours live in `ui/SuitabilityUi.kt`), so it is unit
  tested directly. Everything is scored through one sport-neutral `RuleInput`, which is why
  "now", each day and each hour all go through identical code.
- **Settings** — the thresholds are described once as a list of `Tunable`s in
  [`data/prefs/Tunables.kt`](app/src/main/java/com/zenvairo/sportssuitability/data/prefs/Tunables.kt).
  The settings screen renders from that list and DataStore persists from it, so adding a new
  threshold means adding one entry and nothing else.
- **Persistence** — Room for saved spots (schema v4, migrations 1→2→3→4), DataStore for settings.
- **Network** — Retrofit + kotlinx.serialization, every field nullable so a missing variable
  degrades to "no data" for that metric rather than failing the request.

### A note on units

Instantaneous and accumulated readings are not interchangeable. `rain` is mm **per hour** while
`rain_sum` is mm **per day**, and the thresholds are daily figures — so the "today" verdict takes
wind, waves, temperature and snowpack from the current snapshot but rain and fresh snow from the
day's totals. Hourly verdicts do the same with their parent day. Getting this wrong makes every
precipitation rule silently far too lenient.

## Building

```bash
./gradlew :app:assembleDebug
```

Open the folder in **Android Studio (Koala or newer)**, let Gradle sync (JDK 17), and run on a
device or emulator with API 26+.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

29 JVM unit tests covering the rules engine (hard limits blocking and unblocking, disabling a
limit, the recommended-depth curve, the minimum-score threshold, offshore/onshore detection,
daily-vs-instantaneous unit handling, best-window detection) and the alert scheduler (that the
weekly job lands on the configured evening and rolls over correctly).
