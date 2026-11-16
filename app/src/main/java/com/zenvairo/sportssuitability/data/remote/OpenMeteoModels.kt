package com.zenvairo.sportssuitability.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val country: String? = null,
    val admin1: String? = null,
    val admin2: String? = null
) {
    val regionLabel: String?
        get() = listOfNotNull(admin1, country).joinToString(", ").ifBlank { null }
}

/**
 * Response from `/v1/forecast`. Every field is nullable/defaulted so that a location
 * missing a variable (or an Open-Meteo model that doesn't publish one) degrades to
 * "no data" for that metric instead of failing the whole request.
 */
@Serializable
data class ForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,
    val timezone: String? = null,
    val current: CurrentForecast? = null,
    val hourly: HourlyForecast? = null,
    val daily: DailyForecast? = null
)

@Serializable
data class CurrentForecast(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Double? = null,
    @SerialName("precipitation") val precipitation: Double? = null,
    @SerialName("rain") val rain: Double? = null,
    @SerialName("showers") val showers: Double? = null,
    @SerialName("snowfall") val snowfall: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("cloud_cover") val cloudCover: Double? = null,
    @SerialName("pressure_msl") val pressureMsl: Double? = null,
    @SerialName("surface_pressure") val surfacePressure: Double? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_gusts_10m") val windGusts: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Double? = null,
    @SerialName("is_day") val isDay: Int? = null
)

@Serializable
data class HourlyForecast(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val humidity: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Double?> = emptyList(),
    @SerialName("precipitation") val precipitation: List<Double?> = emptyList(),
    @SerialName("snowfall") val snowfall: List<Double?> = emptyList(),
    @SerialName("snow_depth") val snowDepth: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("cloud_cover") val cloudCover: List<Double?> = emptyList(),
    @SerialName("visibility") val visibility: List<Double?> = emptyList(),
    @SerialName("freezing_level_height") val freezingLevelHeight: List<Double?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m") val windGusts: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m") val windDirection: List<Double?> = emptyList()
)

@Serializable
data class DailyForecast(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_min") val apparentTemperatureMin: List<Double?> = emptyList(),
    @SerialName("sunrise") val sunrise: List<String?> = emptyList(),
    @SerialName("sunset") val sunset: List<String?> = emptyList(),
    @SerialName("daylight_duration") val daylightDuration: List<Double?> = emptyList(),
    @SerialName("sunshine_duration") val sunshineDuration: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @SerialName("rain_sum") val rainSum: List<Double?> = emptyList(),
    @SerialName("showers_sum") val showersSum: List<Double?> = emptyList(),
    @SerialName("snowfall_sum") val snowfallSum: List<Double?> = emptyList(),
    @SerialName("precipitation_hours") val precipitationHours: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m_max") val windGustsMax: List<Double?> = emptyList(),
    @SerialName("wind_direction_10m_dominant") val windDirectionDominant: List<Double?> = emptyList()
)

/** Response from `marine-api.open-meteo.com/v1/marine`. */
@Serializable
data class MarineResponse(
    val current: CurrentMarine? = null,
    val hourly: HourlyMarine? = null,
    val daily: DailyMarine? = null
)

@Serializable
data class CurrentMarine(
    val time: String? = null,
    @SerialName("wave_height") val waveHeight: Double? = null,
    @SerialName("wave_period") val wavePeriod: Double? = null,
    @SerialName("wave_direction") val waveDirection: Double? = null,
    @SerialName("wind_wave_height") val windWaveHeight: Double? = null,
    @SerialName("wind_wave_period") val windWavePeriod: Double? = null,
    @SerialName("wind_wave_direction") val windWaveDirection: Double? = null,
    @SerialName("swell_wave_height") val swellWaveHeight: Double? = null,
    @SerialName("swell_wave_period") val swellWavePeriod: Double? = null,
    @SerialName("swell_wave_peak_period") val swellWavePeakPeriod: Double? = null,
    @SerialName("swell_wave_direction") val swellWaveDirection: Double? = null,
    @SerialName("sea_surface_temperature") val seaSurfaceTemperature: Double? = null,
    @SerialName("sea_level_height_msl") val seaLevelHeightMsl: Double? = null,
    @SerialName("ocean_current_velocity") val oceanCurrentVelocity: Double? = null,
    @SerialName("ocean_current_direction") val oceanCurrentDirection: Double? = null
)

@Serializable
data class HourlyMarine(
    val time: List<String> = emptyList(),
    @SerialName("wave_height") val waveHeight: List<Double?> = emptyList(),
    @SerialName("wave_period") val wavePeriod: List<Double?> = emptyList(),
    @SerialName("wave_direction") val waveDirection: List<Double?> = emptyList(),
    @SerialName("swell_wave_height") val swellWaveHeight: List<Double?> = emptyList(),
    @SerialName("swell_wave_period") val swellWavePeriod: List<Double?> = emptyList(),
    @SerialName("sea_surface_temperature") val seaSurfaceTemperature: List<Double?> = emptyList(),
    @SerialName("sea_level_height_msl") val seaLevelHeightMsl: List<Double?> = emptyList()
)

@Serializable
data class DailyMarine(
    val time: List<String> = emptyList(),
    @SerialName("wave_height_max") val waveHeightMax: List<Double?> = emptyList(),
    @SerialName("wave_period_max") val wavePeriodMax: List<Double?> = emptyList(),
    @SerialName("wave_direction_dominant") val waveDirectionDominant: List<Double?> = emptyList(),
    @SerialName("wind_wave_height_max") val windWaveHeightMax: List<Double?> = emptyList(),
    @SerialName("wind_wave_period_max") val windWavePeriodMax: List<Double?> = emptyList(),
    @SerialName("swell_wave_height_max") val swellWaveHeightMax: List<Double?> = emptyList(),
    @SerialName("swell_wave_period_max") val swellWavePeriodMax: List<Double?> = emptyList(),
    @SerialName("swell_wave_peak_period_max") val swellWavePeakPeriodMax: List<Double?> = emptyList(),
    @SerialName("swell_wave_direction_dominant") val swellWaveDirectionDominant: List<Double?> = emptyList()
)
