package com.zenvairo.sportssuitability.data.repository

import com.zenvairo.sportssuitability.data.local.Spot

/**
 * Everything the app knows about one spot right now and for the coming days.
 *
 * All values are kept in canonical metric units (km/h, m, °C, cm, mm, hPa) so the rules
 * engine never has to convert; display units are applied only at the UI edge.
 */
data class SpotConditions(
    val spot: Spot,
    val now: NowConditions? = null,
    val days: List<DailyConditions> = emptyList(),
    val hours: List<HourlyConditions> = emptyList(),
    val fetchedAtEpochMs: Long = 0L,
    /** Set when nothing could be fetched at all. */
    val error: String? = null,
    /** Set when part of the data (e.g. the marine model) failed but the rest is usable. */
    val partialWarning: String? = null
) {
    val today: DailyConditions? get() = days.firstOrNull()
}

/** The "right now" snapshot. */
data class NowConditions(
    val time: String? = null,
    // Atmosphere
    val temperatureC: Double? = null,
    val apparentTemperatureC: Double? = null,
    val humidityPct: Double? = null,
    val precipitationMm: Double? = null,
    val rainMm: Double? = null,
    val snowfallCm: Double? = null,
    val snowDepthCm: Double? = null,
    val weatherCode: Int? = null,
    val cloudCoverPct: Double? = null,
    val pressureHpa: Double? = null,
    val windSpeedKmh: Double? = null,
    val windGustsKmh: Double? = null,
    val windDirectionDeg: Double? = null,
    val freezingLevelM: Double? = null,
    val visibilityM: Double? = null,
    val uvIndex: Double? = null,
    val isDay: Boolean? = null,
    // Sea
    val waveHeightM: Double? = null,
    val wavePeriodS: Double? = null,
    val waveDirectionDeg: Double? = null,
    val windWaveHeightM: Double? = null,
    val windWavePeriodS: Double? = null,
    val windWaveDirectionDeg: Double? = null,
    val swellHeightM: Double? = null,
    val swellPeriodS: Double? = null,
    val swellPeakPeriodS: Double? = null,
    val swellDirectionDeg: Double? = null,
    val seaTemperatureC: Double? = null,
    val seaLevelM: Double? = null,
    val currentVelocityKmh: Double? = null,
    val currentDirectionDeg: Double? = null
)

/** One day of the outlook. Wind/wave figures are daily maxima; precipitation is a daily sum. */
data class DailyConditions(
    val date: String,
    val weatherCode: Int? = null,
    val temperatureMaxC: Double? = null,
    val temperatureMinC: Double? = null,
    val apparentTemperatureMaxC: Double? = null,
    val apparentTemperatureMinC: Double? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val daylightSeconds: Double? = null,
    val sunshineSeconds: Double? = null,
    val uvIndexMax: Double? = null,
    val precipitationMm: Double? = null,
    val rainMm: Double? = null,
    val precipitationHours: Double? = null,
    val precipitationProbabilityPct: Double? = null,
    val snowfallCm: Double? = null,
    /** Snow lying on the ground, taken as the day's maximum of the hourly series. */
    val snowDepthCm: Double? = null,
    val windSpeedKmh: Double? = null,
    val windGustsKmh: Double? = null,
    val windDirectionDeg: Double? = null,
    val freezingLevelM: Double? = null,
    val visibilityM: Double? = null,
    // Sea
    val waveHeightM: Double? = null,
    val wavePeriodS: Double? = null,
    val waveDirectionDeg: Double? = null,
    val windWaveHeightM: Double? = null,
    val windWavePeriodS: Double? = null,
    val swellHeightM: Double? = null,
    val swellPeriodS: Double? = null,
    val swellPeakPeriodS: Double? = null,
    val swellDirectionDeg: Double? = null,
    val seaTemperatureC: Double? = null,
    val tideExtremes: List<TideExtreme> = emptyList()
) {
    /** Mid-point temperature, handy for a one-glance figure. */
    val temperatureC: Double?
        get() = when {
            temperatureMaxC != null && temperatureMinC != null -> (temperatureMaxC + temperatureMinC) / 2
            else -> temperatureMaxC ?: temperatureMinC
        }
}

/** One hour of the outlook, used for the intraday "best window" strip. */
data class HourlyConditions(
    val time: String,
    val temperatureC: Double? = null,
    val apparentTemperatureC: Double? = null,
    val humidityPct: Double? = null,
    val precipitationMm: Double? = null,
    val precipitationProbabilityPct: Double? = null,
    val snowfallCm: Double? = null,
    val snowDepthCm: Double? = null,
    val weatherCode: Int? = null,
    val cloudCoverPct: Double? = null,
    val visibilityM: Double? = null,
    val freezingLevelM: Double? = null,
    val uvIndex: Double? = null,
    val windSpeedKmh: Double? = null,
    val windGustsKmh: Double? = null,
    val windDirectionDeg: Double? = null,
    val waveHeightM: Double? = null,
    val wavePeriodS: Double? = null,
    val waveDirectionDeg: Double? = null,
    val swellHeightM: Double? = null,
    val swellPeriodS: Double? = null,
    val seaTemperatureC: Double? = null
) {
    val date: String get() = time.substringBefore('T')
    val hourLabel: String get() = time.substringAfter('T').take(5)
}

/** A local high or low tide moment, derived from the hourly sea-level series. */
data class TideExtreme(val time: String, val heightM: Double, val isHigh: Boolean) {
    val hourLabel: String get() = time.substringAfter('T').take(5)
}
