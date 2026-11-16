package com.zenvairo.sportssuitability.data.prefs

import com.zenvairo.sportssuitability.data.local.SportType

/**
 * A single knob of the suitability formulas, described once and reused everywhere: the
 * settings screen renders from this list and [SettingsRepository] persists from it, so
 * adding a new threshold means adding one entry here and nothing else.
 */
enum class TunableKind { WIND, WAVE, TEMP, SNOW, PERIOD, DISTANCE, MM }

sealed interface Tunable {
    val id: String
    val label: String
    val help: String?
    val kind: TunableKind
}

/** A preference that only shifts the score — it can never block a day. */
data class ValueTunable(
    override val id: String,
    override val label: String,
    override val help: String? = null,
    override val kind: TunableKind,
    val get: (AppSettings) -> Double,
    val set: (AppSettings, Double) -> AppSettings
) : Tunable

/** A deal-breaker with its own on/off switch. */
data class LimitTunable(
    override val id: String,
    override val label: String,
    override val help: String? = null,
    override val kind: TunableKind,
    val get: (AppSettings) -> Limit,
    val set: (AppSettings, Limit) -> AppSettings
) : Tunable

data class TunableGroup(
    val title: String,
    val description: String? = null,
    val items: List<Tunable>
)

fun TunableKind.suffix(settings: AppSettings): String = when (this) {
    TunableKind.WIND -> settings.windUnit.suffix
    TunableKind.WAVE -> settings.waveUnit.suffix
    TunableKind.TEMP -> settings.tempUnit.suffix
    TunableKind.SNOW -> settings.snowUnit.suffix
    TunableKind.PERIOD -> "s"
    TunableKind.DISTANCE -> "m"
    TunableKind.MM -> "mm"
}

/** Canonical metric value → the number shown in the text field. */
fun TunableKind.toDisplay(value: Double, settings: AppSettings): Double = when (this) {
    TunableKind.WIND -> value.kmhTo(settings.windUnit)
    TunableKind.WAVE -> value.metersTo(settings.waveUnit)
    TunableKind.TEMP -> value.celsiusTo(settings.tempUnit)
    TunableKind.SNOW -> value.cmTo(settings.snowUnit)
    TunableKind.PERIOD, TunableKind.DISTANCE, TunableKind.MM -> value
}

/** The number the user typed → canonical metric value. */
fun TunableKind.fromDisplay(value: Double, settings: AppSettings): Double = when (this) {
    TunableKind.WIND -> value.toKmhFrom(settings.windUnit)
    TunableKind.WAVE -> value.toMetersFrom(settings.waveUnit)
    TunableKind.TEMP -> value.toCelsiusFrom(settings.tempUnit)
    TunableKind.SNOW -> value.toCmFrom(settings.snowUnit)
    TunableKind.PERIOD, TunableKind.DISTANCE, TunableKind.MM -> value
}

private val surfGroups = listOf(
    TunableGroup(
        title = "Olas",
        description = "El rango ideal marca la puntuación; los límites duros deciden si se puede ir.",
        items = listOf(
            ValueTunable(
                "surf_wave_ideal_min", "Altura ideal mínima", kind = TunableKind.WAVE,
                get = { it.surf.waveIdealMinM },
                set = { s, v -> s.copy(surf = s.surf.copy(waveIdealMinM = v)) }
            ),
            ValueTunable(
                "surf_wave_ideal_max", "Altura ideal máxima", kind = TunableKind.WAVE,
                get = { it.surf.waveIdealMaxM },
                set = { s, v -> s.copy(surf = s.surf.copy(waveIdealMaxM = v)) }
            ),
            LimitTunable(
                "surf_wave_hard_min", "No ir si la ola es menor de",
                help = "Por debajo de esta altura no hay ola con la que surfear.",
                kind = TunableKind.WAVE,
                get = { it.surf.waveHardMinM },
                set = { s, v -> s.copy(surf = s.surf.copy(waveHardMinM = v)) }
            ),
            LimitTunable(
                "surf_wave_hard_max", "No ir si la ola supera",
                kind = TunableKind.WAVE,
                get = { it.surf.waveHardMaxM },
                set = { s, v -> s.copy(surf = s.surf.copy(waveHardMaxM = v)) }
            )
        )
    ),
    TunableGroup(
        title = "Periodo del oleaje",
        description = "Periodos largos = olas con más fuerza y mejor formadas.",
        items = listOf(
            ValueTunable(
                "surf_period_ideal_min", "Periodo ideal mínimo", kind = TunableKind.PERIOD,
                get = { it.surf.periodIdealMinS },
                set = { s, v -> s.copy(surf = s.surf.copy(periodIdealMinS = v)) }
            ),
            LimitTunable(
                "surf_period_hard_min", "No ir si el periodo baja de", kind = TunableKind.PERIOD,
                get = { it.surf.periodHardMinS },
                set = { s, v -> s.copy(surf = s.surf.copy(periodHardMinS = v)) }
            )
        )
    ),
    TunableGroup(
        title = "Viento y agua",
        items = listOf(
            ValueTunable(
                "surf_wind_ideal_max", "Viento ideal máximo",
                help = "Por encima de este valor el mar empieza a desordenarse.",
                kind = TunableKind.WIND,
                get = { it.surf.windIdealMaxKmh },
                set = { s, v -> s.copy(surf = s.surf.copy(windIdealMaxKmh = v)) }
            ),
            LimitTunable(
                "surf_wind_hard_max", "No ir si el viento supera", kind = TunableKind.WIND,
                get = { it.surf.windHardMaxKmh },
                set = { s, v -> s.copy(surf = s.surf.copy(windHardMaxKmh = v)) }
            ),
            LimitTunable(
                "surf_sea_temp_hard_min", "No ir si el agua baja de", kind = TunableKind.TEMP,
                get = { it.surf.seaTempHardMinC },
                set = { s, v -> s.copy(surf = s.surf.copy(seaTempHardMinC = v)) }
            )
        )
    )
)

private val windsurfGroups = listOf(
    TunableGroup(
        title = "Viento",
        description = "En windsurf, quedarse sin viento es tan malo como que sobre.",
        items = listOf(
            ValueTunable(
                "ws_wind_ideal_min", "Viento ideal mínimo", kind = TunableKind.WIND,
                get = { it.windsurf.windIdealMinKmh },
                set = { s, v -> s.copy(windsurf = s.windsurf.copy(windIdealMinKmh = v)) }
            ),
            ValueTunable(
                "ws_wind_ideal_max", "Viento ideal máximo", kind = TunableKind.WIND,
                get = { it.windsurf.windIdealMaxKmh },
                set = { s, v -> s.copy(windsurf = s.windsurf.copy(windIdealMaxKmh = v)) }
            ),
            LimitTunable(
                "ws_wind_hard_min", "No ir si el viento baja de", kind = TunableKind.WIND,
                get = { it.windsurf.windHardMinKmh },
                set = { s, v -> s.copy(windsurf = s.windsurf.copy(windHardMinKmh = v)) }
            ),
            LimitTunable(
                "ws_wind_hard_max", "No ir si el viento supera", kind = TunableKind.WIND,
                get = { it.windsurf.windHardMaxKmh },
                set = { s, v -> s.copy(windsurf = s.windsurf.copy(windHardMaxKmh = v)) }
            ),
            LimitTunable(
                "ws_gust_spread_hard_max", "No ir si racha − viento supera",
                help = "Diferencia entre racha y viento medio: mide lo racheado que está el día.",
                kind = TunableKind.WIND,
                get = { it.windsurf.gustSpreadHardMaxKmh },
                set = { s, v -> s.copy(windsurf = s.windsurf.copy(gustSpreadHardMaxKmh = v)) }
            )
        )
    ),
    TunableGroup(
        title = "Mar y temperatura",
        items = listOf(
            LimitTunable(
                "ws_wave_hard_max", "No ir si la ola supera", kind = TunableKind.WAVE,
                get = { it.windsurf.waveHardMaxM },
                set = { s, v -> s.copy(windsurf = s.windsurf.copy(waveHardMaxM = v)) }
            ),
            LimitTunable(
                "ws_air_temp_hard_min", "No ir si el aire baja de", kind = TunableKind.TEMP,
                get = { it.windsurf.airTempHardMinC },
                set = { s, v -> s.copy(windsurf = s.windsurf.copy(airTempHardMinC = v)) }
            ),
            LimitTunable(
                "ws_sea_temp_hard_min", "No ir si el agua baja de", kind = TunableKind.TEMP,
                get = { it.windsurf.seaTempHardMinC },
                set = { s, v -> s.copy(windsurf = s.windsurf.copy(seaTempHardMinC = v)) }
            )
        )
    )
)

private val skiGroups = listOf(
    TunableGroup(
        title = "Nieve",
        description = "El espesor recomendado es el que puntúa al 100 %; por debajo baja la nota " +
            "hasta el mínimo, que ya impide ir.",
        items = listOf(
            ValueTunable(
                "ski_depth_recommended", "Espesor recomendado", kind = TunableKind.SNOW,
                get = { it.ski.snowDepthRecommendedCm },
                set = { s, v -> s.copy(ski = s.ski.copy(snowDepthRecommendedCm = v)) }
            ),
            LimitTunable(
                "ski_depth_hard_min", "No ir si el espesor baja de", kind = TunableKind.SNOW,
                get = { it.ski.snowDepthHardMinCm },
                set = { s, v -> s.copy(ski = s.ski.copy(snowDepthHardMinCm = v)) }
            ),
            ValueTunable(
                "ski_fresh_ideal", "Nieve fresca para día ideal", kind = TunableKind.SNOW,
                get = { it.ski.freshSnowIdealCm },
                set = { s, v -> s.copy(ski = s.ski.copy(freshSnowIdealCm = v)) }
            )
        )
    ),
    TunableGroup(
        title = "Viento",
        description = "Los remontes cierran con viento fuerte: este es el límite que decide el día.",
        items = listOf(
            ValueTunable(
                "ski_wind_ideal_max", "Viento ideal máximo", kind = TunableKind.WIND,
                get = { it.ski.windIdealMaxKmh },
                set = { s, v -> s.copy(ski = s.ski.copy(windIdealMaxKmh = v)) }
            ),
            LimitTunable(
                "ski_wind_hard_max", "No ir si el viento supera", kind = TunableKind.WIND,
                get = { it.ski.windHardMaxKmh },
                set = { s, v -> s.copy(ski = s.ski.copy(windHardMaxKmh = v)) }
            ),
            LimitTunable(
                "ski_gust_hard_max", "No ir si las rachas superan", kind = TunableKind.WIND,
                get = { it.ski.gustHardMaxKmh },
                set = { s, v -> s.copy(ski = s.ski.copy(gustHardMaxKmh = v)) }
            )
        )
    ),
    TunableGroup(
        title = "Temperatura",
        items = listOf(
            ValueTunable(
                "ski_temp_ideal_min", "Temperatura ideal mínima", kind = TunableKind.TEMP,
                get = { it.ski.tempIdealMinC },
                set = { s, v -> s.copy(ski = s.ski.copy(tempIdealMinC = v)) }
            ),
            ValueTunable(
                "ski_temp_ideal_max", "Temperatura ideal máxima", kind = TunableKind.TEMP,
                get = { it.ski.tempIdealMaxC },
                set = { s, v -> s.copy(ski = s.ski.copy(tempIdealMaxC = v)) }
            ),
            LimitTunable(
                "ski_temp_hard_min", "No ir si baja de", kind = TunableKind.TEMP,
                get = { it.ski.tempHardMinC },
                set = { s, v -> s.copy(ski = s.ski.copy(tempHardMinC = v)) }
            ),
            LimitTunable(
                "ski_temp_hard_max", "No ir si sube de", kind = TunableKind.TEMP,
                get = { it.ski.tempHardMaxC },
                set = { s, v -> s.copy(ski = s.ski.copy(tempHardMaxC = v)) }
            )
        )
    ),
    TunableGroup(
        title = "Lluvia, visibilidad y cota",
        items = listOf(
            LimitTunable(
                "ski_rain_hard_max", "No ir si llueve más de", kind = TunableKind.MM,
                get = { it.ski.rainHardMaxMm },
                set = { s, v -> s.copy(ski = s.ski.copy(rainHardMaxMm = v)) }
            ),
            LimitTunable(
                "ski_visibility_hard_min", "No ir si la visibilidad baja de", kind = TunableKind.DISTANCE,
                get = { it.ski.visibilityHardMinM },
                set = { s, v -> s.copy(ski = s.ski.copy(visibilityHardMinM = v)) }
            ),
            LimitTunable(
                "ski_freezing_level_hard_max", "No ir si la cota de nieve sube de",
                help = "Isoterma cero: si queda por encima de la estación, cae agua en vez de nieve.",
                kind = TunableKind.DISTANCE,
                get = { it.ski.freezingLevelHardMaxM },
                set = { s, v -> s.copy(ski = s.ski.copy(freezingLevelHardMaxM = v)) }
            )
        )
    )
)

fun tunableGroups(sport: SportType): List<TunableGroup> = when (sport) {
    SportType.SURF -> surfGroups
    SportType.WINDSURF -> windsurfGroups
    SportType.SKI -> skiGroups
}

/** Every knob in the app, used to drive persistence generically. */
val allTunables: List<Tunable> =
    SportType.values().flatMap { tunableGroups(it) }.flatMap { it.items }
