package com.zenvairo.sportssuitability.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** No API key needed: Open-Meteo's free tier requires no authentication. */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val geocodingApi: GeocodingApi by lazy {
        retrofit("https://geocoding-api.open-meteo.com/").create(GeocodingApi::class.java)
    }

    val forecastApi: ForecastApi by lazy {
        retrofit("https://api.open-meteo.com/").create(ForecastApi::class.java)
    }

    val marineApi: MarineApi by lazy {
        retrofit("https://marine-api.open-meteo.com/").create(MarineApi::class.java)
    }
}
