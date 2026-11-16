package com.zenvairo.sportssuitability.data.repository

import com.zenvairo.sportssuitability.data.local.Spot
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.Limit
import com.zenvairo.sportssuitability.data.prefs.angleDelta
import com.zenvairo.sportssuitability.data.prefs.formatMm
import com.zenvairo.sportssuitability.data.prefs.formatSnow
import com.zenvairo.sportssuitability.data.prefs.formatSnowDepth
import com.zenvairo.sportssuitability.data.prefs.formatTemp
import com.zenvairo.sportssuitability.data.prefs.formatVisibility
import com.zenvairo.sportssuitability.data.prefs.formatWave
import com.zenvairo.sportssuitability.data.prefs.formatWind
import com.zenvairo.sportssuitability.data.prefs.toCompass
import kotlin.math.roundToInt

/** Colours live in the UI layer ([com.zenvairo.sportssuitability.ui.color]) so the rules
 *  engine stays plain Kotlin and unit-testable. */
enum class Suitability(val label: String) {
    EXCELLENT("Ideal"),
    GOOD("Bueno"),
    FAIR("Regular"),
    POOR("Flojo"),
    NO_GO("No ir"),
    UNKNOWN("Sin datos")
}

/**
 * The answer to "¿puedo ir?" for one spot on one day, with the reasoning attached so the
 * card and the notification can both explain themselves.
 */
data class Verdict(
    val suitability: Suitability,
    /** 0–100, or `null` when there was not enough data to score anything. */
    val score: Int?,
    val canGo: Boolean,
    /** Hard limits that were crossed — each one on its own is enough to stay home. */
    val blockers: List<String> = emptyList(),
    /** Things that are poor but not disqualifying. */
    val warnings: List<String> = emptyList(),
    /** Things that are genuinely good today. */
    val highlights: List<String> = emptyList()
) {
    /** One line suitable for a notification or a card subtitle. */
    val headline: String
        get() = when {
            blockers.isNotEmpty() -> blockers.first()
            highlights.isNotEmpty() -> highlights.joinToString(" · ")
            warnings.isNotEmpty() -> warnings.first()
            else -> suitability.label
        }
}

/** Where the wind blows relative to the coast — the difference between glass and slop. */
enum class WindShore(val label: String) {
    OFFSHORE("Viento offshore (de tierra)"),
    CROSS("Viento cruzado"),
    ONSHORE("Viento onshore (de mar)"),
    UNKNOWN("Orientación no configurada")
}

/**
 * Sport-neutral snapshot the rules run against, so "now" and each forecast day are scored
 * by exactly the same code. All values are canonical metric.
 */
data class RuleInput(
    val windKmh: Double? = null,
    val gustKmh: Double? = null,
    val windDirectionDeg: Double? = null,
    val waveHeightM: Double? = null,
    val wavePeriodS: Double? = null,
    val swellPeriodS: Double? = null,
    val seaTempC: Double? = null,
    val airTempC: Double? = null,
    val precipitationMm: Double? = null,
    val rainMm: Double? = null,
    val snowfallCm: Double? = null,
    val snowDepthCm: Double? = null,
    val visibilityM: Double? = null,
    val freezingLevelM: Double? = null,
    val cloudCoverPct: Double? = null,
    val coastFacingDeg: Double? = null
)

fun NowConditions.toRuleInput(spot: Spot) = RuleInput(
    windKmh = windSpeedKmh,
    gustKmh = windGustsKmh,
    windDirectionDeg = windDirectionDeg,
    waveHeightM = waveHeightM,
    wavePeriodS = wavePeriodS,
    swellPeriodS = swellPeakPeriodS ?: swellPeriodS,
    seaTempC = seaTemperatureC,
    airTempC = temperatureC,
    precipitationMm = precipitationMm,
    rainMm = rainMm ?: precipitationMm,
    snowfallCm = snowfallCm,
    snowDepthCm = snowDepthCm,
    visibilityM = visibilityM,
    freezingLevelM = freezingLevelM,
    cloudCoverPct = cloudCoverPct,
    coastFacingDeg = spot.coastFacingDeg
)

fun DailyConditions.toRuleInput(spot: Spot) = RuleInput(
    windKmh = windSpeedKmh,
    gustKmh = windGustsKmh,
    windDirectionDeg = windDirectionDeg,
    waveHeightM = waveHeightM,
    wavePeriodS = wavePeriodS,
    swellPeriodS = swellPeakPeriodS ?: swellPeriodS,
    seaTempC = seaTemperatureC,
    airTempC = temperatureMaxC ?: temperatureC,
    precipitationMm = precipitationMm,
    rainMm = rainMm ?: precipitationMm,
    snowfallCm = snowfallCm,
    snowDepthCm = snowDepthCm,
    visibilityM = visibilityM,
    freezingLevelM = freezingLevelM,
    coastFacingDeg = spot.coastFacingDeg
)

fun HourlyConditions.toRuleInput(spot: Spot) = RuleInput(
    windKmh = windSpeedKmh,
    gustKmh = windGustsKmh,
    windDirectionDeg = windDirectionDeg,
    waveHeightM = waveHeightM,
    wavePeriodS = wavePeriodS,
    swellPeriodS = swellPeriodS,
    seaTempC = seaTemperatureC,
    airTempC = temperatureC,
    precipitationMm = precipitationMm,
    rainMm = precipitationMm,
    snowfallCm = snowfallCm,
    snowDepthCm = snowDepthCm,
    visibilityM = visibilityM,
    freezingLevelM = freezingLevelM,
    cloudCoverPct = cloudCoverPct,
    coastFacingDeg = spot.coastFacingDeg
)

fun windShore(windDirectionDeg: Double?, coastFacingDeg: Double?): WindShore {
    if (windDirectionDeg == null || coastFacingDeg == null) return WindShore.UNKNOWN
    // Meteorological direction is where the wind comes *from*; offshore wind therefore
    // arrives from the bearing opposite the one the beach looks towards.
    val delta = angleDelta(windDirectionDeg, (coastFacingDeg + 180.0) % 360.0)
    return when {
        delta <= 50 -> WindShore.OFFSHORE
        delta >= 130 -> WindShore.ONSHORE
        else -> WindShore.CROSS
    }
}

// ---------------------------------------------------------------------------
// Scoring primitives. Each returns 0.0 (useless) … 1.0 (perfect).
// ---------------------------------------------------------------------------

/** Full marks inside [min, max], fading to zero over [tolerance] on either side. */
private fun bandScore(value: Double, min: Double, max: Double, tolerance: Double): Double {
    val lo = minOf(min, max)
    val hi = maxOf(min, max)
    val tol = tolerance.coerceAtLeast(0.001)
    return when {
        value in lo..hi -> 1.0
        value < lo -> (1.0 - (lo - value) / tol).coerceIn(0.0, 1.0)
        else -> (1.0 - (value - hi) / tol).coerceIn(0.0, 1.0)
    }
}

/** Zero at or below [floor], full marks at or above [target]. */
private fun atLeastScore(value: Double, floor: Double, target: Double): Double {
    if (target <= floor) return if (value >= target) 1.0 else 0.0
    return ((value - floor) / (target - floor)).coerceIn(0.0, 1.0)
}

/** Full marks at or below [target], zero at or above [ceiling]. */
private fun atMostScore(value: Double, target: Double, ceiling: Double): Double {
    if (ceiling <= target) return if (value <= target) 1.0 else 0.0
    return (1.0 - (value - target) / (ceiling - target)).coerceIn(0.0, 1.0)
}

private data class Factor(
    val label: String,
    val weight: Double,
    val score: Double,
    val detail: String
)

private class Evaluation {
    val factors = mutableListOf<Factor>()
    val blockers = mutableListOf<String>()

    fun factor(label: String, weight: Double, score: Double, detail: String) {
        factors += Factor(label, weight, score.coerceIn(0.0, 1.0), detail)
    }

    /** Records a blocker when [value] rises above an enabled [limit]. */
    fun blockIfAbove(limit: Limit, value: Double?, describe: (Double) -> String) {
        if (limit.enabled && value != null && value > limit.value) blockers += describe(limit.value)
    }

    /** Records a blocker when [value] falls below an enabled [limit]. */
    fun blockIfBelow(limit: Limit, value: Double?, describe: (Double) -> String) {
        if (limit.enabled && value != null && value < limit.value) blockers += describe(limit.value)
    }

    fun toVerdict(settings: AppSettings): Verdict {
        val usable = factors.filter { it.weight > 0 }
        val totalWeight = usable.sumOf { it.weight }
        val score = if (totalWeight <= 0) null else
            (usable.sumOf { it.score * it.weight } / totalWeight * 100).roundToInt()

        val warnings = usable.filter { it.score < 0.4 }
            .sortedBy { it.score }
            .map { "${it.label}: ${it.detail}" }
        val highlights = usable.filter { it.score >= 0.85 && it.weight >= 1.0 }
            .sortedByDescending { it.weight }
            .take(3)
            .map { "${it.label} ${it.detail}" }

        val suitability = when {
            blockers.isNotEmpty() -> Suitability.NO_GO
            score == null -> Suitability.UNKNOWN
            score >= 85 -> Suitability.EXCELLENT
            score >= 70 -> Suitability.GOOD
            score >= settings.minScoreToGo -> Suitability.FAIR
            else -> Suitability.POOR
        }

        return Verdict(
            suitability = suitability,
            score = score,
            canGo = blockers.isEmpty() && score != null && score >= settings.minScoreToGo,
            blockers = blockers.toList(),
            warnings = warnings.take(3),
            highlights = highlights
        )
    }
}

// ---------------------------------------------------------------------------
// Per-sport rules
// ---------------------------------------------------------------------------

private fun evaluateSurf(input: RuleInput, settings: AppSettings): Verdict {
    val rules = settings.surf
    val eval = Evaluation()

    eval.blockIfAbove(rules.waveHardMaxM, input.waveHeightM) {
        "Ola demasiado grande: ${input.waveHeightM!!.formatWave(settings.waveUnit)} " +
            "(límite ${it.formatWave(settings.waveUnit)})"
    }
    eval.blockIfBelow(rules.waveHardMinM, input.waveHeightM) {
        "Sin ola: ${input.waveHeightM!!.formatWave(settings.waveUnit)} " +
            "(mínimo ${it.formatWave(settings.waveUnit)})"
    }
    eval.blockIfBelow(rules.periodHardMinS, input.wavePeriodS) {
        "Periodo demasiado corto: ${"%.1f".format(input.wavePeriodS)} s (mínimo ${"%.1f".format(it)} s)"
    }
    eval.blockIfAbove(rules.windHardMaxKmh, input.windKmh) {
        "Demasiado viento: ${input.windKmh!!.formatWind(settings.windUnit)} " +
            "(límite ${it.formatWind(settings.windUnit)})"
    }
    eval.blockIfBelow(rules.seaTempHardMinC, input.seaTempC) {
        "Agua a ${input.seaTempC!!.formatTemp(settings.tempUnit)} " +
            "(mínimo ${it.formatTemp(settings.tempUnit)})"
    }

    input.waveHeightM?.let { wave ->
        val span = (rules.waveIdealMaxM - rules.waveIdealMinM).coerceAtLeast(0.2)
        eval.factor("Ola", 3.0, bandScore(wave, rules.waveIdealMinM, rules.waveIdealMaxM, span * 0.8),
            wave.formatWave(settings.waveUnit))
    }
    (input.swellPeriodS ?: input.wavePeriodS)?.let { period ->
        eval.factor("Periodo", 2.0,
            atLeastScore(period, rules.periodIdealMinS * 0.55, rules.periodIdealMinS),
            "${"%.1f".format(period)} s")
    }
    input.windKmh?.let { wind ->
        eval.factor("Viento", 2.0,
            atMostScore(wind, rules.windIdealMaxKmh, rules.windIdealMaxKmh * 2.5 + 10),
            wind.formatWind(settings.windUnit))
    }
    if (rules.preferOffshore) {
        val shore = windShore(input.windDirectionDeg, input.coastFacingDeg)
        val score = when (shore) {
            WindShore.OFFSHORE -> 1.0
            WindShore.CROSS -> 0.55
            WindShore.ONSHORE -> 0.15
            WindShore.UNKNOWN -> -1.0
        }
        if (score >= 0) {
            eval.factor("Dirección del viento", 1.5, score,
                input.windDirectionDeg?.toCompass()?.let { "$it · ${shore.label}" } ?: shore.label)
        }
    }
    input.seaTempC?.let { eval.factor("Agua", 0.5, atLeastScore(it, 8.0, 18.0), it.formatTemp(settings.tempUnit)) }

    return eval.toVerdict(settings)
}

private fun evaluateWindsurf(input: RuleInput, settings: AppSettings): Verdict {
    val rules = settings.windsurf
    val eval = Evaluation()
    val gustSpread = if (input.gustKmh != null && input.windKmh != null)
        (input.gustKmh - input.windKmh).coerceAtLeast(0.0) else null

    eval.blockIfAbove(rules.windHardMaxKmh, input.windKmh) {
        "Demasiado viento: ${input.windKmh!!.formatWind(settings.windUnit)} " +
            "(límite ${it.formatWind(settings.windUnit)})"
    }
    eval.blockIfBelow(rules.windHardMinKmh, input.windKmh) {
        "Sin viento: ${input.windKmh!!.formatWind(settings.windUnit)} " +
            "(mínimo ${it.formatWind(settings.windUnit)})"
    }
    eval.blockIfAbove(rules.gustSpreadHardMaxKmh, gustSpread) {
        "Viento muy racheado: +${gustSpread!!.formatWind(settings.windUnit)} de racha " +
            "(límite ${it.formatWind(settings.windUnit)})"
    }
    eval.blockIfAbove(rules.waveHardMaxM, input.waveHeightM) {
        "Mar demasiado formado: ${input.waveHeightM!!.formatWave(settings.waveUnit)} " +
            "(límite ${it.formatWave(settings.waveUnit)})"
    }
    eval.blockIfBelow(rules.airTempHardMinC, input.airTempC) {
        "Aire a ${input.airTempC!!.formatTemp(settings.tempUnit)} " +
            "(mínimo ${it.formatTemp(settings.tempUnit)})"
    }
    eval.blockIfBelow(rules.seaTempHardMinC, input.seaTempC) {
        "Agua a ${input.seaTempC!!.formatTemp(settings.tempUnit)} " +
            "(mínimo ${it.formatTemp(settings.tempUnit)})"
    }

    input.windKmh?.let { wind ->
        val span = (rules.windIdealMaxKmh - rules.windIdealMinKmh).coerceAtLeast(5.0)
        eval.factor("Viento", 4.0,
            bandScore(wind, rules.windIdealMinKmh, rules.windIdealMaxKmh, span * 0.9),
            wind.formatWind(settings.windUnit))
    }
    gustSpread?.let { spread ->
        val ceiling = if (rules.gustSpreadHardMaxKmh.enabled) rules.gustSpreadHardMaxKmh.value else 30.0
        eval.factor("Constancia", 1.5, atMostScore(spread, 8.0, ceiling),
            "rachas +${spread.formatWind(settings.windUnit)}")
    }
    if (rules.preferSideShore) {
        val shore = windShore(input.windDirectionDeg, input.coastFacingDeg)
        val score = when (shore) {
            WindShore.CROSS -> 1.0
            WindShore.ONSHORE -> 0.75
            WindShore.OFFSHORE -> 0.3
            WindShore.UNKNOWN -> -1.0
        }
        if (score >= 0) {
            eval.factor("Dirección del viento", 1.5, score,
                input.windDirectionDeg?.toCompass()?.let { "$it · ${shore.label}" } ?: shore.label)
        }
    }
    input.airTempC?.let {
        eval.factor("Temperatura", 0.75, atLeastScore(it, 4.0, 18.0), it.formatTemp(settings.tempUnit))
    }
    input.waveHeightM?.let {
        val ceiling = if (rules.waveHardMaxM.enabled) rules.waveHardMaxM.value else 3.0
        eval.factor("Mar", 0.5, atMostScore(it, 1.2, ceiling), it.formatWave(settings.waveUnit))
    }

    return eval.toVerdict(settings)
}

private fun evaluateSki(input: RuleInput, settings: AppSettings): Verdict {
    val rules = settings.ski
    val eval = Evaluation()

    eval.blockIfAbove(rules.windHardMaxKmh, input.windKmh) {
        "Viento ${input.windKmh!!.formatWind(settings.windUnit)} " +
            "(límite ${it.formatWind(settings.windUnit)}): remontes en riesgo de cierre"
    }
    eval.blockIfAbove(rules.gustHardMaxKmh, input.gustKmh) {
        "Rachas de ${input.gustKmh!!.formatWind(settings.windUnit)} " +
            "(límite ${it.formatWind(settings.windUnit)})"
    }
    eval.blockIfBelow(rules.snowDepthHardMinCm, input.snowDepthCm) {
        "Espesor de ${input.snowDepthCm!!.formatSnowDepth(settings.snowUnit)} " +
            "(mínimo ${it.formatSnowDepth(settings.snowUnit)})"
    }
    eval.blockIfAbove(rules.tempHardMaxC, input.airTempC) {
        "Demasiado calor: ${input.airTempC!!.formatTemp(settings.tempUnit)} " +
            "(límite ${it.formatTemp(settings.tempUnit)})"
    }
    eval.blockIfBelow(rules.tempHardMinC, input.airTempC) {
        "Demasiado frío: ${input.airTempC!!.formatTemp(settings.tempUnit)} " +
            "(mínimo ${it.formatTemp(settings.tempUnit)})"
    }
    eval.blockIfAbove(rules.rainHardMaxMm, input.rainMm) {
        "Lluvia de ${input.rainMm!!.formatMm()} (límite ${it.formatMm()})"
    }
    eval.blockIfBelow(rules.visibilityHardMinM, input.visibilityM) {
        "Visibilidad de ${input.visibilityM!!.formatVisibility()} (mínimo ${it.formatVisibility()})"
    }
    eval.blockIfAbove(rules.freezingLevelHardMaxM, input.freezingLevelM) {
        "Cota de nieve a ${input.freezingLevelM!!.roundToInt()} m (límite ${it.roundToInt()} m)"
    }

    input.snowDepthCm?.let { depth ->
        val floor = if (rules.snowDepthHardMinCm.enabled) rules.snowDepthHardMinCm.value else 0.0
        eval.factor("Espesor", 3.0,
            atLeastScore(depth, floor, rules.snowDepthRecommendedCm),
            depth.formatSnowDepth(settings.snowUnit))
    }
    input.snowfallCm?.let { fresh ->
        // No fresh snow is not a disaster on a groomed piste, so this factor starts at 0.4.
        val score = 0.4 + 0.6 * atLeastScore(fresh, 0.0, rules.freshSnowIdealCm)
        eval.factor("Nieve fresca", 2.0, score, fresh.formatSnow(settings.snowUnit))
    }
    input.windKmh?.let { wind ->
        val ceiling = if (rules.windHardMaxKmh.enabled) rules.windHardMaxKmh.value
        else rules.windIdealMaxKmh * 2.5
        eval.factor("Viento", 2.0, atMostScore(wind, rules.windIdealMaxKmh, ceiling),
            wind.formatWind(settings.windUnit))
    }
    input.airTempC?.let { temp ->
        eval.factor("Temperatura", 1.5,
            bandScore(temp, rules.tempIdealMinC, rules.tempIdealMaxC, 8.0),
            temp.formatTemp(settings.tempUnit))
    }
    input.rainMm?.let { rain ->
        val ceiling = if (rules.rainHardMaxMm.enabled) rules.rainHardMaxMm.value else 10.0
        eval.factor("Lluvia", 1.5, atMostScore(rain, 0.4, ceiling), rain.formatMm())
    }
    input.visibilityM?.let { vis ->
        val floor = if (rules.visibilityHardMinM.enabled) rules.visibilityHardMinM.value else 200.0
        eval.factor("Visibilidad", 1.0, atLeastScore(vis, floor, 8000.0), vis.formatVisibility())
    }
    input.cloudCoverPct?.let { cloud ->
        eval.factor("Cielo", 0.5, atMostScore(cloud, 25.0, 100.0), "${cloud.roundToInt()} % nubes")
    }

    return eval.toVerdict(settings)
}

fun evaluate(sportType: SportType, input: RuleInput, settings: AppSettings): Verdict =
    when (sportType) {
        SportType.SURF -> evaluateSurf(input, settings)
        SportType.WINDSURF -> evaluateWindsurf(input, settings)
        SportType.SKI -> evaluateSki(input, settings)
    }

/**
 * The input behind "¿puedo ir hoy?".
 *
 * Instantaneous readings (wind, waves, temperature, snowpack) come from the "now" snapshot,
 * but rain and fresh snow are taken from today's totals: the thresholds are expressed per day
 * — 8 mm of rain, 10 cm of fresh snow — and comparing them against a mm-per-hour reading
 * would silently make every rule far too lenient.
 */
private fun SpotConditions.todayInput(): RuleInput? {
    val fromNow = now?.toRuleInput(spot)
    val fromToday = today?.toRuleInput(spot)
    if (fromNow == null) return fromToday
    if (fromToday == null) return fromNow

    return fromNow.copy(
        windKmh = fromNow.windKmh ?: fromToday.windKmh,
        gustKmh = fromNow.gustKmh ?: fromToday.gustKmh,
        windDirectionDeg = fromNow.windDirectionDeg ?: fromToday.windDirectionDeg,
        waveHeightM = fromNow.waveHeightM ?: fromToday.waveHeightM,
        wavePeriodS = fromNow.wavePeriodS ?: fromToday.wavePeriodS,
        swellPeriodS = fromNow.swellPeriodS ?: fromToday.swellPeriodS,
        seaTempC = fromNow.seaTempC ?: fromToday.seaTempC,
        airTempC = fromNow.airTempC ?: fromToday.airTempC,
        snowDepthCm = fromNow.snowDepthCm ?: fromToday.snowDepthCm,
        visibilityM = fromNow.visibilityM ?: fromToday.visibilityM,
        freezingLevelM = fromNow.freezingLevelM ?: fromToday.freezingLevelM,
        cloudCoverPct = fromNow.cloudCoverPct ?: fromToday.cloudCoverPct,
        // Daily totals win for anything that accumulates.
        precipitationMm = fromToday.precipitationMm ?: fromNow.precipitationMm,
        rainMm = fromToday.rainMm ?: fromNow.rainMm,
        snowfallCm = fromToday.snowfallCm ?: fromNow.snowfallCm
    )
}

/**
 * Verdict for today. Falls back to whichever of "now" and "today" is available, so a spot
 * never shows "sin datos" just because one endpoint was quiet.
 */
fun SpotConditions.nowVerdict(settings: AppSettings): Verdict {
    val input = todayInput() ?: return Verdict(Suitability.UNKNOWN, null, canGo = false)
    return evaluate(spot.sportType, input, settings)
}

fun SpotConditions.dailyVerdict(day: DailyConditions, settings: AppSettings): Verdict =
    evaluate(spot.sportType, day.toRuleInput(spot), settings)

/**
 * Verdict for a single hour. Accumulating figures are taken from the hour's parent day for
 * the same reason as [todayInput]: 10 cm of fresh snow is a daily figure, not an hourly one.
 */
fun SpotConditions.hourlyVerdict(hour: HourlyConditions, settings: AppSettings): Verdict {
    val parentDay = days.firstOrNull { it.date == hour.date }
    val input = hour.toRuleInput(spot).copy(
        precipitationMm = parentDay?.precipitationMm ?: hour.precipitationMm,
        rainMm = parentDay?.rainMm ?: hour.precipitationMm,
        snowfallCm = parentDay?.snowfallCm ?: hour.snowfallCm
    )
    return evaluate(spot.sportType, input, settings)
}

/**
 * Longest unbroken run of go-able hours on [date] — the window worth aiming for.
 * Returns the first and last hour labels, or null when no hour makes the cut.
 */
fun SpotConditions.bestWindow(date: String, settings: AppSettings): Pair<String, String>? {
    val onDate = hours.filter { it.date == date }
    if (onDate.isEmpty()) return null

    var bestStart = -1
    var bestLength = 0
    var runStart = -1
    onDate.forEachIndexed { index, hour ->
        if (hourlyVerdict(hour, settings).canGo) {
            if (runStart < 0) runStart = index
            val length = index - runStart + 1
            if (length > bestLength) {
                bestLength = length
                bestStart = runStart
            }
        } else {
            runStart = -1
        }
    }
    if (bestLength == 0) return null
    return onDate[bestStart].hourLabel to onDate[bestStart + bestLength - 1].hourLabel
}
