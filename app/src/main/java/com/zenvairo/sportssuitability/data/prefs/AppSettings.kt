package com.zenvairo.sportssuitability.data.prefs

/**
 * A deal-breaker threshold. When [enabled] and crossed, the day is rated "No ir" no matter
 * how good everything else looks — e.g. ski wind above 70 km/h closes the lifts.
 * Turning it off downgrades the check to an ordinary scoring factor.
 */
data class Limit(val enabled: Boolean, val value: Double)

/**
 * Surf rules. "Ideal" bounds shape the score; "hard" limits can veto the day outright.
 * Wave figures are significant height in metres, wind in km/h, period in seconds.
 */
data class SurfRules(
    val waveIdealMinM: Double = 0.8,
    val waveIdealMaxM: Double = 2.5,
    val waveHardMinM: Limit = Limit(enabled = true, value = 0.4),
    val waveHardMaxM: Limit = Limit(enabled = true, value = 4.0),
    val periodIdealMinS: Double = 9.0,
    val periodHardMinS: Limit = Limit(enabled = false, value = 6.0),
    val windIdealMaxKmh: Double = 15.0,
    val windHardMaxKmh: Limit = Limit(enabled = true, value = 40.0),
    val seaTempHardMinC: Limit = Limit(enabled = false, value = 12.0),
    /** Reward offshore wind (needs the spot's coast orientation to be set). */
    val preferOffshore: Boolean = true
)

/** Windsurf rules: it is the only sport where too *little* wind is itself a deal-breaker. */
data class WindsurfRules(
    val windIdealMinKmh: Double = 25.0,
    val windIdealMaxKmh: Double = 45.0,
    val windHardMinKmh: Limit = Limit(enabled = true, value = 16.0),
    val windHardMaxKmh: Limit = Limit(enabled = true, value = 65.0),
    /** Gust minus average wind: a big spread means unrideable, gusty air. */
    val gustSpreadHardMaxKmh: Limit = Limit(enabled = true, value = 25.0),
    val waveHardMaxM: Limit = Limit(enabled = false, value = 2.5),
    val airTempHardMinC: Limit = Limit(enabled = false, value = 5.0),
    val seaTempHardMinC: Limit = Limit(enabled = false, value = 10.0),
    /** Reward side / side-onshore wind, penalise straight offshore (it blows you out to sea). */
    val preferSideShore: Boolean = true
)

/**
 * Ski rules. [snowDepthRecommendedCm] is the configurable "espesor recomendado": at or above
 * it the snowpack scores full marks, below it the score tapers off towards
 * [snowDepthHardMinCm], which vetoes the day when enabled.
 */
data class SkiRules(
    val snowDepthRecommendedCm: Double = 60.0,
    val snowDepthHardMinCm: Limit = Limit(enabled = true, value = 20.0),
    val freshSnowIdealCm: Double = 10.0,
    val windIdealMaxKmh: Double = 30.0,
    val windHardMaxKmh: Limit = Limit(enabled = true, value = 70.0),
    val gustHardMaxKmh: Limit = Limit(enabled = true, value = 90.0),
    val tempIdealMinC: Double = -12.0,
    val tempIdealMaxC: Double = 2.0,
    val tempHardMinC: Limit = Limit(enabled = true, value = -20.0),
    val tempHardMaxC: Limit = Limit(enabled = true, value = 8.0),
    val rainHardMaxMm: Limit = Limit(enabled = true, value = 8.0),
    val visibilityHardMinM: Limit = Limit(enabled = true, value = 300.0),
    /** Freezing level above this means rain rather than snow on the slopes. */
    val freezingLevelHardMaxM: Limit = Limit(enabled = false, value = 2400.0)
)

/** When and how the app is allowed to interrupt you. */
data class AlertSettings(
    val dailyEnabled: Boolean = true,
    val dailyHour: Int = 7,
    val dailyMinute: Int = 0,
    /** When false, the morning alert also tells you when a spot is a no-go. */
    val dailyOnlyWhenGood: Boolean = false,
    val weeklyEnabled: Boolean = true,
    /** ISO day of week, 1 = Monday … 7 = Sunday. */
    val weeklyDayOfWeek: Int = 7,
    val weeklyHour: Int = 18,
    val weeklyMinute: Int = 0
)

/**
 * User-configurable units, thresholds and alert schedule.
 * Thresholds are always stored in the canonical metric unit (km/h, m, cm, °C, mm)
 * regardless of the chosen display unit, so the rules engine never converts.
 */
data class AppSettings(
    val windUnit: WindUnit = WindUnit.KMH,
    val waveUnit: WaveUnit = WaveUnit.METERS,
    val tempUnit: TempUnit = TempUnit.CELSIUS,
    val snowUnit: SnowUnit = SnowUnit.CM,

    val surf: SurfRules = SurfRules(),
    val windsurf: WindsurfRules = WindsurfRules(),
    val ski: SkiRules = SkiRules(),
    val alerts: AlertSettings = AlertSettings(),

    /** Score (0–100) a day must reach before the app says "sí, puedes ir". */
    val minScoreToGo: Int = 55
)
