package com.zenvairo.sportssuitability.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenvairo.sportssuitability.data.local.AppDatabase
import com.zenvairo.sportssuitability.data.local.SportType
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.SettingsRepository
import com.zenvairo.sportssuitability.data.repository.SpotConditions
import com.zenvairo.sportssuitability.data.repository.SpotsRepository
import com.zenvairo.sportssuitability.data.repository.dailyVerdict
import com.zenvairo.sportssuitability.ui.uiInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Notification ids for the weekly digest, kept clear of the per-spot daily ids. */
private const val WEEKLY_NOTIFICATION_ID_BASE = 900_000

/**
 * The Sunday-evening digest: one notification per sport, listing every saved place and the
 * days of the coming week that are actually worth going, so the week can be planned in one go.
 */
class WeeklyForecastWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = SettingsRepository(applicationContext).settings.first()
        try {
            ensureNotificationChannels(applicationContext)
            val repository = SpotsRepository(AppDatabase.get(applicationContext))
            val spots = repository.allSpots()
            if (spots.isEmpty()) return@withContext Result.success()

            val conditions = coroutineScope {
                spots.map { async { repository.fetchConditions(it) } }.awaitAll()
            }

            conditions.groupBy { it.spot.sportType }.forEach { (sport, forSport) ->
                notifySport(sport, forSport, settings)
            }
            Result.success()
        } catch (e: Exception) {
            // See DailyAlertWorker: the finally block re-arms, so a retry would only race it.
            Result.success()
        } finally {
            AlertScheduler.scheduleWeekly(applicationContext, settings.alerts)
        }
    }

    private fun notifySport(
        sport: SportType,
        conditions: List<SpotConditions>,
        settings: AppSettings
    ) {
        val today = LocalDate.now()
        val label = sport.uiInfo().label
        var anyGoodDay = false

        val lines = conditions.map { spotConditions ->
            val name = spotConditions.spot.name
            if (spotConditions.error != null) return@map "$name: sin datos"

            // Skip today — this runs on Sunday evening, the point is the week ahead.
            val upcoming = spotConditions.days.filter { day ->
                runCatching { LocalDate.parse(day.date).isAfter(today) }.getOrDefault(false)
            }
            val good = upcoming
                .map { it to spotConditions.dailyVerdict(it, settings) }
                .filter { (_, verdict) -> verdict.canGo }
                .sortedByDescending { (_, verdict) -> verdict.score ?: 0 }
                .take(3)

            if (good.isEmpty()) {
                "$name: ningún día recomendable"
            } else {
                anyGoodDay = true
                val days = good.joinToString(", ") { (day, verdict) ->
                    "${shortDayLabel(day.date, today)} ${verdict.suitability.label.lowercase()}" +
                        (verdict.score?.let { " ($it)" } ?: "")
                }
                "$name: $days"
            }
        }

        val summary = if (anyGoodDay) {
            "Hay días buenos esta semana"
        } else {
            "Semana floja: ningún día llega al mínimo"
        }

        postNotification(
            context = applicationContext,
            channelId = WEEKLY_CHANNEL_ID,
            notificationId = WEEKLY_NOTIFICATION_ID_BASE + sport.ordinal,
            title = "$label — previsión de la semana",
            summary = summary,
            body = lines.joinToString("\n")
        )
    }
}
