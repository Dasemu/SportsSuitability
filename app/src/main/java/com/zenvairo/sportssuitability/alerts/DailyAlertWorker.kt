package com.zenvairo.sportssuitability.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zenvairo.sportssuitability.data.local.AppDatabase
import com.zenvairo.sportssuitability.data.local.Spot
import com.zenvairo.sportssuitability.data.prefs.AppSettings
import com.zenvairo.sportssuitability.data.prefs.SettingsRepository
import com.zenvairo.sportssuitability.data.repository.SpotConditions
import com.zenvairo.sportssuitability.data.repository.SpotsRepository
import com.zenvairo.sportssuitability.data.repository.Verdict
import com.zenvairo.sportssuitability.data.repository.dailyVerdict
import com.zenvairo.sportssuitability.data.repository.nowVerdict
import com.zenvairo.sportssuitability.ui.uiInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Runs at the configured hour every morning and answers, per spot, "¿hoy puedo ir?".
 * Unless the user opted into good-news-only, a no-go is reported too, with the reason —
 * knowing not to drive two hours is worth as much as knowing to go.
 */
class DailyAlertWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settings = SettingsRepository(applicationContext).settings.first()
        try {
            ensureNotificationChannels(applicationContext)
            val repository = SpotsRepository(AppDatabase.get(applicationContext))
            repository.spotsWithAlerts().forEach { spot ->
                notifyFor(repository.fetchConditions(spot), spot, settings)
            }
            Result.success()
        } catch (e: Exception) {
            // Not a retry: the finally block already queues tomorrow's run, and a WorkManager
            // retry would race with it over the same unique work name.
            Result.success()
        } finally {
            // Re-arm tomorrow's run whatever happened, so one bad night doesn't end the series.
            AlertScheduler.scheduleDaily(applicationContext, settings.alerts)
        }
    }

    private fun notifyFor(conditions: SpotConditions, spot: Spot, settings: AppSettings) {
        if (conditions.error != null) return
        // Prefer today's daily figures: at 07:00 the "current" snapshot says little about
        // whether the afternoon is worth the drive.
        val verdict = conditions.today?.let { conditions.dailyVerdict(it, settings) }
            ?: conditions.nowVerdict(settings)

        if (settings.alerts.dailyOnlyWhenGood && !verdict.canGo) return
        if (verdict.score == null && verdict.blockers.isEmpty()) return

        val sportLabel = spot.sportType.uiInfo().label
        val title = if (verdict.canGo) "✅ Hoy sí — ${spot.name}" else "🚫 Hoy no — ${spot.name}"
        postNotification(
            context = applicationContext,
            channelId = DAILY_CHANNEL_ID,
            notificationId = spot.id.toInt(),
            title = title,
            summary = verdict.headline,
            body = buildBody(sportLabel, verdict)
        )
    }

    private fun buildBody(sportLabel: String, verdict: Verdict): String = buildString {
        append(sportLabel)
        append(" · ")
        append(verdict.suitability.label)
        verdict.score?.let { append(" (") ; append(it) ; append("/100)") }
        if (verdict.blockers.isNotEmpty()) {
            append("\n\nPor qué no:")
            verdict.blockers.forEach { append("\n• ").append(it) }
        }
        if (verdict.highlights.isNotEmpty()) {
            append("\n\nA favor:")
            verdict.highlights.forEach { append("\n• ").append(it) }
        }
        if (verdict.warnings.isNotEmpty()) {
            append("\n\nOjo:")
            verdict.warnings.forEach { append("\n• ").append(it) }
        }
    }
}
