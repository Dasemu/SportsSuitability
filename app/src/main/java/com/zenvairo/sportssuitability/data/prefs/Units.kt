package com.zenvairo.sportssuitability.data.prefs

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Unit choices per measurement kind. Raw values from Open-Meteo are always kept in the
 * canonical metric unit (km/h, metres, °C, cm) — these enums only affect display and input.
 */
enum class WindUnit(val label: String, val suffix: String) {
    KMH("km/h", "km/h"),
    KNOTS("nudos", "kt"),
    MS("m/s", "m/s"),
    MPH("mph", "mph")
}

enum class WaveUnit(val label: String, val suffix: String) {
    METERS("metros", "m"),
    FEET("pies", "ft")
}

enum class TempUnit(val label: String, val suffix: String) {
    CELSIUS("Celsius", "°C"),
    FAHRENHEIT("Fahrenheit", "°F")
}

enum class SnowUnit(val label: String, val suffix: String) {
    CM("centímetros", "cm"),
    INCHES("pulgadas", "in")
}

fun Double.kmhTo(unit: WindUnit): Double = when (unit) {
    WindUnit.KMH -> this
    WindUnit.KNOTS -> this * 0.539957
    WindUnit.MS -> this / 3.6
    WindUnit.MPH -> this * 0.621371
}

fun Double.toKmhFrom(unit: WindUnit): Double = when (unit) {
    WindUnit.KMH -> this
    WindUnit.KNOTS -> this / 0.539957
    WindUnit.MS -> this * 3.6
    WindUnit.MPH -> this / 0.621371
}

fun Double.metersTo(unit: WaveUnit): Double = when (unit) {
    WaveUnit.METERS -> this
    WaveUnit.FEET -> this * 3.28084
}

fun Double.toMetersFrom(unit: WaveUnit): Double = when (unit) {
    WaveUnit.METERS -> this
    WaveUnit.FEET -> this / 3.28084
}

fun Double.celsiusTo(unit: TempUnit): Double = when (unit) {
    TempUnit.CELSIUS -> this
    TempUnit.FAHRENHEIT -> this * 9.0 / 5.0 + 32.0
}

/** Converts an *absolute* temperature typed by the user back to °C. */
fun Double.toCelsiusFrom(unit: TempUnit): Double = when (unit) {
    TempUnit.CELSIUS -> this
    TempUnit.FAHRENHEIT -> (this - 32.0) * 5.0 / 9.0
}

fun Double.cmTo(unit: SnowUnit): Double = when (unit) {
    SnowUnit.CM -> this
    SnowUnit.INCHES -> this / 2.54
}

fun Double.toCmFrom(unit: SnowUnit): Double = when (unit) {
    SnowUnit.CM -> this
    SnowUnit.INCHES -> this * 2.54
}

private fun Double.decimals(n: Int) = "%.${n}f".format(this)

fun Double.formatWind(unit: WindUnit): String = "${kmhTo(unit).decimals(0)} ${unit.suffix}"
fun Double.formatWave(unit: WaveUnit): String = "${metersTo(unit).decimals(1)} ${unit.suffix}"
fun Double.formatTemp(unit: TempUnit): String = "${celsiusTo(unit).decimals(0)}${unit.suffix}"
fun Double.formatSnow(unit: SnowUnit): String = "${cmTo(unit).decimals(1)} ${unit.suffix}"
fun Double.formatSnowDepth(unit: SnowUnit): String = "${cmTo(unit).decimals(0)} ${unit.suffix}"

fun Double.formatPeriod(): String = "${decimals(1)} s"
fun Double.formatMm(): String = "${decimals(1)} mm"
fun Double.formatPercent(): String = "${roundToInt()} %"
fun Double.formatHpa(): String = "${roundToInt()} hPa"
fun Double.formatAltitude(): String = "${roundToInt()} m"
fun Double.formatUv(): String = decimals(1)

fun Double.formatVisibility(): String =
    if (this >= 1000) "${(this / 1000).decimals(1)} km" else "${roundToInt()} m"

fun Double.formatDuration(): String {
    val totalMinutes = (this / 60).roundToInt()
    return "${totalMinutes / 60} h ${totalMinutes % 60} min"
}

private val COMPASS_ES = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO"
)

/** Turns a bearing into a Spanish compass point, e.g. 225° → "SO". */
fun Double.toCompass(): String {
    val normalised = ((this % 360) + 360) % 360
    return COMPASS_ES[((normalised / 22.5).roundToInt()) % 16]
}

fun Double.formatDirection(): String = "${toCompass()} (${roundToInt()}°)"

/** Smallest angle between two bearings, always in 0..180. */
fun angleDelta(a: Double, b: Double): Double {
    val diff = abs(((a - b) % 360 + 360) % 360)
    return if (diff > 180) 360 - diff else diff
}
