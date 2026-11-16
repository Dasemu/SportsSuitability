package com.zenvairo.sportssuitability.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

private object Keys {
    val WIND_UNIT = stringPreferencesKey("wind_unit")
    val WAVE_UNIT = stringPreferencesKey("wave_unit")
    val TEMP_UNIT = stringPreferencesKey("temp_unit")
    val SNOW_UNIT = stringPreferencesKey("snow_unit")

    val SURF_PREFER_OFFSHORE = booleanPreferencesKey("surf_prefer_offshore")
    val WINDSURF_PREFER_SIDESHORE = booleanPreferencesKey("windsurf_prefer_sideshore")
    val MIN_SCORE_TO_GO = intPreferencesKey("min_score_to_go")

    val DAILY_ENABLED = booleanPreferencesKey("alert_daily_enabled")
    val DAILY_HOUR = intPreferencesKey("alert_daily_hour")
    val DAILY_MINUTE = intPreferencesKey("alert_daily_minute")
    val DAILY_ONLY_GOOD = booleanPreferencesKey("alert_daily_only_good")
    val WEEKLY_ENABLED = booleanPreferencesKey("alert_weekly_enabled")
    val WEEKLY_DOW = intPreferencesKey("alert_weekly_dow")
    val WEEKLY_HOUR = intPreferencesKey("alert_weekly_hour")
    val WEEKLY_MINUTE = intPreferencesKey("alert_weekly_minute")

    fun tunableValue(id: String) = doublePreferencesKey("tunable_$id")
    fun tunableEnabled(id: String) = booleanPreferencesKey("tunable_${id}_on")
}

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toAppSettings() }

    /** Applies [transform] and returns the stored result, so callers never read a stale flow. */
    suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        lateinit var result: AppSettings
        context.dataStore.edit { prefs ->
            val updated = transform(prefs.toAppSettings())
            result = updated

            prefs[Keys.WIND_UNIT] = updated.windUnit.name
            prefs[Keys.WAVE_UNIT] = updated.waveUnit.name
            prefs[Keys.TEMP_UNIT] = updated.tempUnit.name
            prefs[Keys.SNOW_UNIT] = updated.snowUnit.name

            prefs[Keys.SURF_PREFER_OFFSHORE] = updated.surf.preferOffshore
            prefs[Keys.WINDSURF_PREFER_SIDESHORE] = updated.windsurf.preferSideShore
            prefs[Keys.MIN_SCORE_TO_GO] = updated.minScoreToGo

            prefs[Keys.DAILY_ENABLED] = updated.alerts.dailyEnabled
            prefs[Keys.DAILY_HOUR] = updated.alerts.dailyHour
            prefs[Keys.DAILY_MINUTE] = updated.alerts.dailyMinute
            prefs[Keys.DAILY_ONLY_GOOD] = updated.alerts.dailyOnlyWhenGood
            prefs[Keys.WEEKLY_ENABLED] = updated.alerts.weeklyEnabled
            prefs[Keys.WEEKLY_DOW] = updated.alerts.weeklyDayOfWeek
            prefs[Keys.WEEKLY_HOUR] = updated.alerts.weeklyHour
            prefs[Keys.WEEKLY_MINUTE] = updated.alerts.weeklyMinute

            allTunables.forEach { tunable ->
                when (tunable) {
                    is ValueTunable -> prefs[Keys.tunableValue(tunable.id)] = tunable.get(updated)
                    is LimitTunable -> {
                        val limit = tunable.get(updated)
                        prefs[Keys.tunableValue(tunable.id)] = limit.value
                        prefs[Keys.tunableEnabled(tunable.id)] = limit.enabled
                    }
                }
            }
        }
        return result
    }

    /** Restores every threshold of one sport to the app defaults, leaving units untouched. */
    suspend fun resetSportDefaults(sport: com.zenvairo.sportssuitability.data.local.SportType) {
        update { current ->
            val defaults = AppSettings()
            tunableGroups(sport).flatMap { it.items }.fold(current) { acc, tunable ->
                when (tunable) {
                    is ValueTunable -> tunable.set(acc, tunable.get(defaults))
                    is LimitTunable -> tunable.set(acc, tunable.get(defaults))
                }
            }
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        val base = defaults.copy(
            windUnit = enumOr(Keys.WIND_UNIT, defaults.windUnit) { WindUnit.valueOf(it) },
            waveUnit = enumOr(Keys.WAVE_UNIT, defaults.waveUnit) { WaveUnit.valueOf(it) },
            tempUnit = enumOr(Keys.TEMP_UNIT, defaults.tempUnit) { TempUnit.valueOf(it) },
            snowUnit = enumOr(Keys.SNOW_UNIT, defaults.snowUnit) { SnowUnit.valueOf(it) },
            minScoreToGo = (this[Keys.MIN_SCORE_TO_GO] ?: defaults.minScoreToGo).coerceIn(0, 100),
            surf = defaults.surf.copy(
                preferOffshore = this[Keys.SURF_PREFER_OFFSHORE] ?: defaults.surf.preferOffshore
            ),
            windsurf = defaults.windsurf.copy(
                preferSideShore = this[Keys.WINDSURF_PREFER_SIDESHORE]
                    ?: defaults.windsurf.preferSideShore
            ),
            alerts = AlertSettings(
                dailyEnabled = this[Keys.DAILY_ENABLED] ?: defaults.alerts.dailyEnabled,
                dailyHour = (this[Keys.DAILY_HOUR] ?: defaults.alerts.dailyHour).coerceIn(0, 23),
                dailyMinute = (this[Keys.DAILY_MINUTE] ?: defaults.alerts.dailyMinute).coerceIn(0, 59),
                dailyOnlyWhenGood = this[Keys.DAILY_ONLY_GOOD] ?: defaults.alerts.dailyOnlyWhenGood,
                weeklyEnabled = this[Keys.WEEKLY_ENABLED] ?: defaults.alerts.weeklyEnabled,
                weeklyDayOfWeek = (this[Keys.WEEKLY_DOW] ?: defaults.alerts.weeklyDayOfWeek).coerceIn(1, 7),
                weeklyHour = (this[Keys.WEEKLY_HOUR] ?: defaults.alerts.weeklyHour).coerceIn(0, 23),
                weeklyMinute = (this[Keys.WEEKLY_MINUTE] ?: defaults.alerts.weeklyMinute).coerceIn(0, 59)
            )
        )

        return allTunables.fold(base) { acc, tunable ->
            val stored = this[Keys.tunableValue(tunable.id)]
            when (tunable) {
                is ValueTunable -> stored?.let { tunable.set(acc, it) } ?: acc
                is LimitTunable -> {
                    val fallback = tunable.get(defaults)
                    tunable.set(
                        acc,
                        Limit(
                            enabled = this[Keys.tunableEnabled(tunable.id)] ?: fallback.enabled,
                            value = stored ?: fallback.value
                        )
                    )
                }
            }
        }
    }

    private inline fun <T> Preferences.enumOr(
        key: Preferences.Key<String>,
        fallback: T,
        parse: (String) -> T
    ): T = this[key]?.let { runCatching { parse(it) }.getOrNull() } ?: fallback
}
