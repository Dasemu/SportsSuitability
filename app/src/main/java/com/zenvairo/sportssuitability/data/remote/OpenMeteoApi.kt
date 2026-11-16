package com.zenvairo.sportssuitability.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** Variable lists are centralised so the request and the parsing model stay in sync. */
object OpenMeteoVars {
    const val FORECAST_CURRENT =
        "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,rain,showers," +
            "snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure," +
            "wind_speed_10m,wind_gusts_10m,wind_direction_10m,is_day"

    const val FORECAST_HOURLY =
        "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability," +
            "precipitation,snowfall,snow_depth,weather_code,cloud_cover,visibility," +
            "freezing_level_height,uv_index,wind_speed_10m,wind_gusts_10m,wind_direction_10m"

    const val FORECAST_DAILY =
        "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max," +
            "apparent_temperature_min,sunrise,sunset,daylight_duration,sunshine_duration," +
            "uv_index_max,precipitation_sum,rain_sum,showers_sum,snowfall_sum,precipitation_hours," +
            "precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max," +
            "wind_direction_10m_dominant"

    const val MARINE_CURRENT =
        "wave_height,wave_period,wave_direction,wind_wave_height,wind_wave_period," +
            "wind_wave_direction,swell_wave_height,swell_wave_period,swell_wave_peak_period," +
            "swell_wave_direction,sea_surface_temperature,sea_level_height_msl," +
            "ocean_current_velocity,ocean_current_direction"

    const val MARINE_HOURLY =
        "wave_height,wave_period,wave_direction,swell_wave_height,swell_wave_period," +
            "sea_surface_temperature,sea_level_height_msl"

    const val MARINE_DAILY =
        "wave_height_max,wave_period_max,wave_direction_dominant,wind_wave_height_max," +
            "wind_wave_period_max,swell_wave_height_max,swell_wave_period_max," +
            "swell_wave_peak_period_max,swell_wave_direction_dominant"
}

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "es",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

/**
 * `api.open-meteo.com` — atmosphere. One request returns "now", the hourly detail and the
 * daily outlook, so a spot needs a single round trip instead of three.
 */
interface ForecastApi {
    @GET("v1/forecast")
    suspend fun bundle(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = OpenMeteoVars.FORECAST_CURRENT,
        @Query("hourly") hourly: String = OpenMeteoVars.FORECAST_HOURLY,
        @Query("daily") daily: String = OpenMeteoVars.FORECAST_DAILY,
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 8,
        @Query("elevation") elevation: Double? = null
    ): ForecastResponse
}

/** `marine-api.open-meteo.com` — waves, swell, sea temperature and sea level (tides). */
interface MarineApi {
    @GET("v1/marine")
    suspend fun bundle(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = OpenMeteoVars.MARINE_CURRENT,
        @Query("hourly") hourly: String = OpenMeteoVars.MARINE_HOURLY,
        @Query("daily") daily: String = OpenMeteoVars.MARINE_DAILY,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 8
    ): MarineResponse
}
