package com.zenvairo.sportssuitability.ui

import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.formatAltitude
import com.zenvairo.sportssuitability.data.prefs.formatDirection
import com.zenvairo.sportssuitability.data.prefs.formatDuration
import com.zenvairo.sportssuitability.data.prefs.formatHpa
import com.zenvairo.sportssuitability.data.prefs.formatMm
import com.zenvairo.sportssuitability.data.prefs.formatPercent
import com.zenvairo.sportssuitability.data.prefs.formatPeriod
import com.zenvairo.sportssuitability.data.prefs.formatSnow
import com.zenvairo.sportssuitability.data.prefs.formatSnowDepth
import com.zenvairo.sportssuitability.data.prefs.formatTemp
import com.zenvairo.sportssuitability.data.prefs.formatUv
import com.zenvairo.sportssuitability.data.prefs.formatVisibility
import com.zenvairo.sportssuitability.data.prefs.formatWave
import com.zenvairo.sportssuitability.data.prefs.formatWind
import com.zenvairo.sportssuitability.data.repository.DailyConditions
import com.zenvairo.sportssuitability.data.repository.SpotConditions
import com.zenvairo.sportssuitability.data.repository.weatherCodeText
import com.zenvairo.sportssuitability.data.repository.windShore
import com.zenvairo.sportssuitability.data.repository.WindShore

/** One labelled reading, ready to be rendered as a chip. */
data class Metric(val label: String, val value: String)

data class MetricSection(val title: String, val metrics: List<Metric>)

private fun sectionOf(title: String, vararg metrics: Metric?): MetricSection? {
    val present = metrics.filterNotNull()
    return if (present.isEmpty()) null else MetricSection(title, present)
}

private fun metric(label: String, value: String?): Metric? =
    value?.let { Metric(label, it) }

/**
 * Every reading the app has for "right now", grouped so the section that matters most to
 * the sport comes first. Missing readings simply disappear rather than showing blanks.
 */
fun SpotConditions.nowSections(settings: AppSettings): List<MetricSection> {
    val now = now ?: return emptyList()
    val day = today

    val sea = sectionOf(
        "Mar",
        metric("Altura de ola", now.waveHeightM?.formatWave(settings.waveUnit)),
        metric("Periodo", now.wavePeriodS?.formatPeriod()),
        metric("Dirección de la ola", now.waveDirectionDeg?.formatDirection()),
        metric("Mar de fondo", now.swellHeightM?.formatWave(settings.waveUnit)),
        metric("Periodo del fondo", (now.swellPeakPeriodS ?: now.swellPeriodS)?.formatPeriod()),
        metric("Dirección del fondo", now.swellDirectionDeg?.formatDirection()),
        metric("Mar de viento", now.windWaveHeightM?.formatWave(settings.waveUnit)),
        metric("Periodo del mar de viento", now.windWavePeriodS?.formatPeriod()),
        metric("Temperatura del agua", now.seaTemperatureC?.formatTemp(settings.tempUnit)),
        metric("Corriente", now.currentVelocityKmh?.formatWind(settings.windUnit)),
        metric("Dirección de la corriente", now.currentDirectionDeg?.formatDirection()),
        metric("Nivel del mar", now.seaLevelM?.let { "%.2f m".format(it) })
    )

    val shore = windShore(now.windDirectionDeg, spot.coastFacingDeg)
    val wind = sectionOf(
        "Viento",
        metric("Viento", now.windSpeedKmh?.formatWind(settings.windUnit)),
        metric("Rachas", now.windGustsKmh?.formatWind(settings.windUnit)),
        metric("Dirección", now.windDirectionDeg?.formatDirection()),
        metric("Respecto a la costa", shore.label.takeIf { shore != WindShore.UNKNOWN })
    )

    val snow = sectionOf(
        "Nieve",
        metric("Espesor", now.snowDepthCm?.formatSnowDepth(settings.snowUnit)),
        metric("Nieve fresca (ahora)", now.snowfallCm?.formatSnow(settings.snowUnit)),
        metric("Nieve fresca (hoy)", day?.snowfallCm?.formatSnow(settings.snowUnit)),
        metric("Cota de nieve", now.freezingLevelM?.formatAltitude()),
        metric("Altitud del punto", spot.altitudeM?.formatAltitude())
    )

    val air = sectionOf(
        "Aire",
        metric("Cielo", weatherCodeText(now.weatherCode)),
        metric("Temperatura", now.temperatureC?.formatTemp(settings.tempUnit)),
        metric("Sensación térmica", now.apparentTemperatureC?.formatTemp(settings.tempUnit)),
        metric("Humedad", now.humidityPct?.formatPercent()),
        metric("Nubosidad", now.cloudCoverPct?.formatPercent()),
        metric("Precipitación", now.precipitationMm?.formatMm()),
        metric("Presión", now.pressureHpa?.formatHpa()),
        metric("Visibilidad", now.visibilityM?.formatVisibility()),
        metric("Índice UV", now.uvIndex?.formatUv())
    )

    val sun = sectionOf(
        "Sol",
        metric("Amanecer", day?.sunrise?.substringAfter('T')?.take(5)),
        metric("Atardecer", day?.sunset?.substringAfter('T')?.take(5)),
        metric("Horas de luz", day?.daylightSeconds?.formatDuration()),
        metric("Horas de sol", day?.sunshineSeconds?.formatDuration()),
        metric("UV máximo hoy", day?.uvIndexMax?.formatUv())
    )

    val tides = day?.tideExtremes
        ?.takeIf { it.isNotEmpty() }
        ?.let { extremes ->
            MetricSection(
                "Mareas de hoy",
                extremes.map {
                    Metric(
                        if (it.isHigh) "Pleamar ${it.hourLabel}" else "Bajamar ${it.hourLabel}",
                        "%.2f m".format(it.heightM)
                    )
                }
            )
        }

    val ordered = when (spot.sportType) {
        SportType.SURF -> listOf(sea, wind, tides, air, sun)
        SportType.WINDSURF -> listOf(wind, sea, air, tides, sun)
        SportType.SKI -> listOf(snow, wind, air, sun)
    }
    return ordered.filterNotNull()
}

/** The same treatment for one forecast day. */
fun SpotConditions.daySections(day: DailyConditions, settings: AppSettings): List<MetricSection> {
    val sea = sectionOf(
        "Mar",
        metric("Ola máxima", day.waveHeightM?.formatWave(settings.waveUnit)),
        metric("Periodo máximo", day.wavePeriodS?.formatPeriod()),
        metric("Dirección dominante", day.waveDirectionDeg?.formatDirection()),
        metric("Mar de fondo", day.swellHeightM?.formatWave(settings.waveUnit)),
        metric("Periodo del fondo", (day.swellPeakPeriodS ?: day.swellPeriodS)?.formatPeriod()),
        metric("Dirección del fondo", day.swellDirectionDeg?.formatDirection()),
        metric("Mar de viento", day.windWaveHeightM?.formatWave(settings.waveUnit)),
        metric("Temperatura del agua", day.seaTemperatureC?.formatTemp(settings.tempUnit))
    )

    val shore = windShore(day.windDirectionDeg, spot.coastFacingDeg)
    val wind = sectionOf(
        "Viento",
        metric("Viento máximo", day.windSpeedKmh?.formatWind(settings.windUnit)),
        metric("Rachas máximas", day.windGustsKmh?.formatWind(settings.windUnit)),
        metric("Dirección dominante", day.windDirectionDeg?.formatDirection()),
        metric("Respecto a la costa", shore.label.takeIf { shore != WindShore.UNKNOWN })
    )

    val snow = sectionOf(
        "Nieve",
        metric("Espesor máximo", day.snowDepthCm?.formatSnowDepth(settings.snowUnit)),
        metric("Nieve fresca", day.snowfallCm?.formatSnow(settings.snowUnit)),
        metric("Cota de nieve media", day.freezingLevelM?.formatAltitude())
    )

    val air = sectionOf(
        "Aire",
        metric("Cielo", weatherCodeText(day.weatherCode)),
        metric("Máxima", day.temperatureMaxC?.formatTemp(settings.tempUnit)),
        metric("Mínima", day.temperatureMinC?.formatTemp(settings.tempUnit)),
        metric("Sensación máxima", day.apparentTemperatureMaxC?.formatTemp(settings.tempUnit)),
        metric("Sensación mínima", day.apparentTemperatureMinC?.formatTemp(settings.tempUnit)),
        metric("Precipitación", day.precipitationMm?.formatMm()),
        metric("Probabilidad de lluvia", day.precipitationProbabilityPct?.formatPercent()),
        metric("Horas de precipitación", day.precipitationHours?.let { "${it.toInt()} h" }),
        metric("Visibilidad mínima", day.visibilityM?.formatVisibility())
    )

    val sun = sectionOf(
        "Sol",
        metric("Amanecer", day.sunrise?.substringAfter('T')?.take(5)),
        metric("Atardecer", day.sunset?.substringAfter('T')?.take(5)),
        metric("Horas de luz", day.daylightSeconds?.formatDuration()),
        metric("Horas de sol", day.sunshineSeconds?.formatDuration()),
        metric("UV máximo", day.uvIndexMax?.formatUv())
    )

    val tides = day.tideExtremes.takeIf { it.isNotEmpty() }?.let { extremes ->
        MetricSection(
            "Mareas",
            extremes.map {
                Metric(
                    if (it.isHigh) "Pleamar ${it.hourLabel}" else "Bajamar ${it.hourLabel}",
                    "%.2f m".format(it.heightM)
                )
            }
        )
    }

    val ordered = when (spot.sportType) {
        SportType.SURF -> listOf(sea, wind, tides, air, sun)
        SportType.WINDSURF -> listOf(wind, sea, air, tides, sun)
        SportType.SKI -> listOf(snow, wind, air, sun)
    }
    return ordered.filterNotNull()
}

/** The three or four readings that belong on the collapsed card, chosen per sport. */
fun SpotConditions.headlineMetrics(settings: AppSettings): List<Metric> {
    val now = now ?: return emptyList()
    val day = today
    return when (spot.sportType) {
        SportType.SURF -> listOfNotNull(
            metric("Ola", now.waveHeightM?.formatWave(settings.waveUnit)),
            metric("Periodo", (now.swellPeakPeriodS ?: now.wavePeriodS)?.formatPeriod()),
            metric("Viento", now.windSpeedKmh?.formatWind(settings.windUnit)),
            metric("Agua", now.seaTemperatureC?.formatTemp(settings.tempUnit))
        )
        SportType.WINDSURF -> listOfNotNull(
            metric("Viento", now.windSpeedKmh?.formatWind(settings.windUnit)),
            metric("Rachas", now.windGustsKmh?.formatWind(settings.windUnit)),
            metric("Dirección", now.windDirectionDeg?.formatDirection()),
            metric("Ola", now.waveHeightM?.formatWave(settings.waveUnit))
        )
        SportType.SKI -> listOfNotNull(
            metric("Espesor", now.snowDepthCm?.formatSnowDepth(settings.snowUnit)),
            metric("Fresca hoy", day?.snowfallCm?.formatSnow(settings.snowUnit)),
            metric("Viento", now.windSpeedKmh?.formatWind(settings.windUnit)),
            metric("Temperatura", now.temperatureC?.formatTemp(settings.tempUnit))
        )
    }
}
