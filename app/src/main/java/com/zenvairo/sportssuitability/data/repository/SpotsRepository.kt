package com.zenvairo.sportssuitability.data.repository

import com.zenvairo.sportssuitability.data.local.AppDatabase
import com.zenvairo.sportssuitability.data.local.Spot
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.remote.ForecastResponse
import com.zenvairo.sportssuitability.data.remote.GeocodingResult
import com.zenvairo.sportssuitability.data.remote.HourlyForecast
import com.zenvairo.sportssuitability.data.remote.HourlyMarine
import com.zenvairo.sportssuitability.data.remote.MarineResponse
import com.zenvairo.sportssuitability.data.remote.NetworkModule
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

/** Snow depth arrives from Open-Meteo in metres; everything downstream works in centimetres. */
private const val METRES_TO_CM = 100.0

class SpotsRepository(db: AppDatabase) {

    private val dao = db.spotDao()

    fun observeSpots(sportType: SportType): Flow<List<Spot>> = dao.observeBySport(sportType)

    suspend fun allSpots(): List<Spot> = dao.allSpots()

    suspend fun addSpot(result: GeocodingResult, sportType: SportType) {
        dao.insert(
            Spot(
                name = result.name,
                region = result.regionLabel,
                latitude = result.latitude,
                longitude = result.longitude,
                sportType = sportType,
                altitudeM = result.elevation.takeIf { sportType == SportType.SKI }
            )
        )
    }

    suspend fun removeSpot(spot: Spot) = dao.delete(spot)

    suspend fun setAlertsEnabled(spot: Spot, enabled: Boolean) = dao.setAlertsEnabled(spot.id, enabled)

    suspend fun setCoastFacing(spot: Spot, facingDeg: Double?) = dao.setCoastFacing(spot.id, facingDeg)

    suspend fun setAltitude(spot: Spot, altitudeM: Double?) = dao.setAltitude(spot.id, altitudeM)

    suspend fun spotsWithAlerts(): List<Spot> = dao.spotsWithAlerts()

    suspend fun searchLocations(query: String): List<GeocodingResult> {
        if (query.isBlank()) return emptyList()
        return NetworkModule.geocodingApi.search(query).results.orEmpty()
    }

    /**
     * One network round trip per relevant API, in parallel. The atmosphere is fetched for
     * every sport (wind matters to surfers too); the marine model is added for water sports.
     * If only one of the two fails the spot still renders, with a warning attached.
     */
    suspend fun fetchConditions(spot: Spot): SpotConditions = coroutineScope {
        val needsMarine = spot.sportType == SportType.SURF || spot.sportType == SportType.WINDSURF

        val forecastJob = async {
            runCatching {
                NetworkModule.forecastApi.bundle(
                    latitude = spot.latitude,
                    longitude = spot.longitude,
                    elevation = spot.altitudeM.takeIf { spot.sportType == SportType.SKI }
                )
            }
        }
        val marineJob = async {
            if (!needsMarine) null
            else runCatching { NetworkModule.marineApi.bundle(spot.latitude, spot.longitude) }
        }

        val forecast = forecastJob.await()
        val marine = marineJob.await()

        val forecastData = forecast.getOrNull()
        val marineData = marine?.getOrNull()

        if (forecastData == null && marineData == null) {
            val cause = forecast.exceptionOrNull() ?: marine?.exceptionOrNull()
            return@coroutineScope SpotConditions(
                spot = spot,
                error = cause?.message ?: "No se pudieron obtener los datos",
                fetchedAtEpochMs = System.currentTimeMillis()
            )
        }

        val partial = when {
            forecastData == null -> "Sin datos meteorológicos; solo se muestra el mar."
            needsMarine && marineData == null -> "Sin datos del modelo marino; solo se muestra el tiempo."
            else -> null
        }

        SpotConditions(
            spot = spot,
            now = buildNow(forecastData, marineData),
            days = buildDays(forecastData, marineData),
            hours = buildHours(forecastData?.hourly, marineData?.hourly),
            fetchedAtEpochMs = System.currentTimeMillis(),
            partialWarning = partial
        )
    }

    // -----------------------------------------------------------------------
    // Mapping
    // -----------------------------------------------------------------------

    private fun buildNow(forecast: ForecastResponse?, marine: MarineResponse?): NowConditions? {
        val current = forecast?.current
        val sea = marine?.current
        if (current == null && sea == null) return null

        // These three only exist in the hourly series, so read them at the current hour.
        val hourly = forecast?.hourly
        val index = hourly?.let { nearestHourIndex(it.time, current?.time) }

        return NowConditions(
            time = current?.time ?: sea?.time,
            temperatureC = current?.temperature,
            apparentTemperatureC = current?.apparentTemperature,
            humidityPct = current?.humidity,
            precipitationMm = current?.precipitation,
            rainMm = current?.rain?.let { rain -> rain + (current.showers ?: 0.0) },
            snowfallCm = current?.snowfall,
            snowDepthCm = index?.let { hourly.snowDepth.getOrNull(it) }?.times(METRES_TO_CM),
            weatherCode = current?.weatherCode,
            cloudCoverPct = current?.cloudCover,
            pressureHpa = current?.pressureMsl ?: current?.surfacePressure,
            windSpeedKmh = current?.windSpeed,
            windGustsKmh = current?.windGusts,
            windDirectionDeg = current?.windDirection,
            freezingLevelM = index?.let { hourly.freezingLevelHeight.getOrNull(it) },
            visibilityM = index?.let { hourly.visibility.getOrNull(it) },
            uvIndex = index?.let { hourly.uvIndex.getOrNull(it) },
            isDay = current?.isDay?.let { it == 1 },
            waveHeightM = sea?.waveHeight,
            wavePeriodS = sea?.wavePeriod,
            waveDirectionDeg = sea?.waveDirection,
            windWaveHeightM = sea?.windWaveHeight,
            windWavePeriodS = sea?.windWavePeriod,
            windWaveDirectionDeg = sea?.windWaveDirection,
            swellHeightM = sea?.swellWaveHeight,
            swellPeriodS = sea?.swellWavePeriod,
            swellPeakPeriodS = sea?.swellWavePeakPeriod,
            swellDirectionDeg = sea?.swellWaveDirection,
            seaTemperatureC = sea?.seaSurfaceTemperature,
            seaLevelM = sea?.seaLevelHeightMsl,
            // Open-Meteo already reports ocean current velocity in km/h.
            currentVelocityKmh = sea?.oceanCurrentVelocity,
            currentDirectionDeg = sea?.oceanCurrentDirection
        )
    }

    private fun buildDays(forecast: ForecastResponse?, marine: MarineResponse?): List<DailyConditions> {
        val daily = forecast?.daily
        val hourlyStats = hourlyDailyStats(forecast?.hourly)
        val tidesByDate = extractTideExtremes(marine?.hourly).groupBy { it.time.substringBefore('T') }
        val seaTempByDate = averageByDate(
            marine?.hourly?.time.orEmpty(),
            marine?.hourly?.seaSurfaceTemperature.orEmpty()
        )

        val marineDaily = marine?.daily
        val marineIndexByDate = marineDaily?.time.orEmpty()
            .withIndex().associate { (i, date) -> date to i }

        // The atmospheric series defines the calendar; ocean-only spots fall back to it.
        val dates = daily?.time?.takeIf { it.isNotEmpty() } ?: marineDaily?.time.orEmpty()

        return dates.mapIndexed { i, date ->
            val stats = hourlyStats[date]
            val m = marineIndexByDate[date]
            DailyConditions(
                date = date,
                weatherCode = daily?.weatherCode?.getOrNull(i),
                temperatureMaxC = daily?.temperatureMax?.getOrNull(i),
                temperatureMinC = daily?.temperatureMin?.getOrNull(i),
                apparentTemperatureMaxC = daily?.apparentTemperatureMax?.getOrNull(i),
                apparentTemperatureMinC = daily?.apparentTemperatureMin?.getOrNull(i),
                sunrise = daily?.sunrise?.getOrNull(i),
                sunset = daily?.sunset?.getOrNull(i),
                daylightSeconds = daily?.daylightDuration?.getOrNull(i),
                sunshineSeconds = daily?.sunshineDuration?.getOrNull(i),
                uvIndexMax = daily?.uvIndexMax?.getOrNull(i),
                precipitationMm = daily?.precipitationSum?.getOrNull(i),
                rainMm = daily?.rainSum?.getOrNull(i)?.let { rain ->
                    rain + (daily.showersSum.getOrNull(i) ?: 0.0)
                },
                precipitationHours = daily?.precipitationHours?.getOrNull(i),
                precipitationProbabilityPct = daily?.precipitationProbabilityMax?.getOrNull(i),
                snowfallCm = daily?.snowfallSum?.getOrNull(i),
                snowDepthCm = stats?.maxSnowDepthCm,
                windSpeedKmh = daily?.windSpeedMax?.getOrNull(i),
                windGustsKmh = daily?.windGustsMax?.getOrNull(i),
                windDirectionDeg = daily?.windDirectionDominant?.getOrNull(i),
                freezingLevelM = stats?.meanFreezingLevelM,
                visibilityM = stats?.minVisibilityM,
                waveHeightM = m?.let { marineDaily?.waveHeightMax?.getOrNull(it) },
                wavePeriodS = m?.let { marineDaily?.wavePeriodMax?.getOrNull(it) },
                waveDirectionDeg = m?.let { marineDaily?.waveDirectionDominant?.getOrNull(it) },
                windWaveHeightM = m?.let { marineDaily?.windWaveHeightMax?.getOrNull(it) },
                windWavePeriodS = m?.let { marineDaily?.windWavePeriodMax?.getOrNull(it) },
                swellHeightM = m?.let { marineDaily?.swellWaveHeightMax?.getOrNull(it) },
                swellPeriodS = m?.let { marineDaily?.swellWavePeriodMax?.getOrNull(it) },
                swellPeakPeriodS = m?.let { marineDaily?.swellWavePeakPeriodMax?.getOrNull(it) },
                swellDirectionDeg = m?.let { marineDaily?.swellWaveDirectionDominant?.getOrNull(it) },
                seaTemperatureC = seaTempByDate[date],
                tideExtremes = tidesByDate[date].orEmpty()
            )
        }
    }

    private fun buildHours(hourly: HourlyForecast?, marine: HourlyMarine?): List<HourlyConditions> {
        val marineIndexByTime = marine?.time.orEmpty()
            .withIndex().associate { (i, time) -> time to i }
        val times = hourly?.time?.takeIf { it.isNotEmpty() } ?: marine?.time.orEmpty()

        return times.mapIndexed { i, time ->
            val m = marineIndexByTime[time]
            HourlyConditions(
                time = time,
                temperatureC = hourly?.temperature?.getOrNull(i),
                apparentTemperatureC = hourly?.apparentTemperature?.getOrNull(i),
                humidityPct = hourly?.humidity?.getOrNull(i),
                precipitationMm = hourly?.precipitation?.getOrNull(i),
                precipitationProbabilityPct = hourly?.precipitationProbability?.getOrNull(i),
                snowfallCm = hourly?.snowfall?.getOrNull(i),
                snowDepthCm = hourly?.snowDepth?.getOrNull(i)?.times(METRES_TO_CM),
                weatherCode = hourly?.weatherCode?.getOrNull(i),
                cloudCoverPct = hourly?.cloudCover?.getOrNull(i),
                visibilityM = hourly?.visibility?.getOrNull(i),
                freezingLevelM = hourly?.freezingLevelHeight?.getOrNull(i),
                uvIndex = hourly?.uvIndex?.getOrNull(i),
                windSpeedKmh = hourly?.windSpeed?.getOrNull(i),
                windGustsKmh = hourly?.windGusts?.getOrNull(i),
                windDirectionDeg = hourly?.windDirection?.getOrNull(i),
                waveHeightM = m?.let { marine?.waveHeight?.getOrNull(it) },
                wavePeriodS = m?.let { marine?.wavePeriod?.getOrNull(it) },
                waveDirectionDeg = m?.let { marine?.waveDirection?.getOrNull(it) },
                swellHeightM = m?.let { marine?.swellWaveHeight?.getOrNull(it) },
                swellPeriodS = m?.let { marine?.swellWavePeriod?.getOrNull(it) },
                seaTemperatureC = m?.let { marine?.seaSurfaceTemperature?.getOrNull(it) }
            )
        }
    }

    // -----------------------------------------------------------------------
    // Hourly → daily helpers
    // -----------------------------------------------------------------------

    private data class HourlyDayStats(
        val maxSnowDepthCm: Double?,
        val meanFreezingLevelM: Double?,
        val minVisibilityM: Double?
    )

    /** Rolls up the variables Open-Meteo only publishes hourly into per-day figures. */
    private fun hourlyDailyStats(hourly: HourlyForecast?): Map<String, HourlyDayStats> {
        if (hourly == null || hourly.time.isEmpty()) return emptyMap()
        return hourly.time.indices
            .groupBy { hourly.time[it].substringBefore('T') }
            .mapValues { (_, indices) ->
                HourlyDayStats(
                    maxSnowDepthCm = indices.mapNotNull { hourly.snowDepth.getOrNull(it) }
                        .maxOrNull()?.times(METRES_TO_CM),
                    meanFreezingLevelM = indices.mapNotNull { hourly.freezingLevelHeight.getOrNull(it) }
                        .takeIf { it.isNotEmpty() }?.average(),
                    minVisibilityM = indices.mapNotNull { hourly.visibility.getOrNull(it) }.minOrNull()
                )
            }
    }

    private fun averageByDate(times: List<String>, values: List<Double?>): Map<String, Double> =
        times.indices
            .groupBy { times[it].substringBefore('T') }
            .mapNotNull { (date, indices) ->
                indices.mapNotNull { values.getOrNull(it) }
                    .takeIf { it.isNotEmpty() }?.let { date to it.average() }
            }
            .toMap()

    /** Index of the hourly slot matching [target] (or the first slot when it can't be matched). */
    private fun nearestHourIndex(times: List<String>, target: String?): Int? {
        if (times.isEmpty()) return null
        if (target == null) return 0
        val hourKey = target.take(13) // yyyy-MM-ddTHH
        val exact = times.indexOfFirst { it.startsWith(hourKey) }
        return if (exact >= 0) exact else 0
    }

    /**
     * Local extrema of the hourly sea-level curve, which approximate the tide turns.
     * Open-Meteo has no tide table on the free tier; sea level MSL is the closest stand-in.
     */
    private fun extractTideExtremes(hourly: HourlyMarine?): List<TideExtreme> {
        if (hourly == null || hourly.time.size < 3) return emptyList()
        val extremes = mutableListOf<TideExtreme>()
        for (i in 1 until hourly.time.size - 1) {
            val prev = hourly.seaLevelHeightMsl.getOrNull(i - 1) ?: continue
            val curr = hourly.seaLevelHeightMsl.getOrNull(i) ?: continue
            val next = hourly.seaLevelHeightMsl.getOrNull(i + 1) ?: continue
            val isPeak = curr > prev && curr >= next
            val isTrough = curr < prev && curr <= next
            if (isPeak || isTrough) {
                extremes += TideExtreme(time = hourly.time[i], heightM = curr, isHigh = isPeak)
            }
        }
        return extremes
    }
}
