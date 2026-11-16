package com.zenvairo.sportssuitability.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.zenvairo.sportssuitability.data.prefs.AlertSettings
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Schedules the two alerts as self-renewing one-shot jobs rather than periodic work.
 *
 * WorkManager's periodic requests drift and cannot be pinned to a wall-clock time, which
 * matters here: "domingo por la tarde" has to actually land on Sunday afternoon. Each worker
 * re-arms the next run when it finishes, so the time stays exact across DST changes and the
 * user can move the hour in settings and have it take effect immediately.
 */
object AlertScheduler {

    const val DAILY_WORK_NAME = "daily_suitability_alert"
    const val WEEKLY_WORK_NAME = "weekly_forecast_alert"

    /**
     * The periodic request used before the switch to self-renewing one-shot work. Installs
     * upgrading from that version still have it enqueued, and it runs the same worker — so
     * without cancelling it the morning alert would fire twice.
     */
    private const val LEGACY_PERIODIC_WORK_NAME = "daily_suitability_alert_check"

    private val networkRequired = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun rescheduleAll(context: Context, alerts: AlertSettings) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
        scheduleDaily(context, alerts)
        scheduleWeekly(context, alerts)
    }

    fun scheduleDaily(context: Context, alerts: AlertSettings, from: LocalDateTime = LocalDateTime.now()) {
        val workManager = WorkManager.getInstance(context)
        if (!alerts.dailyEnabled) {
            workManager.cancelUniqueWork(DAILY_WORK_NAME)
            return
        }
        val delay = untilNextDailyRun(alerts, from)
        workManager.enqueueUniqueWork(
            DAILY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DailyAlertWorker>()
                .setInitialDelay(delay)
                .setConstraints(networkRequired)
                .build()
        )
    }

    fun scheduleWeekly(context: Context, alerts: AlertSettings, from: LocalDateTime = LocalDateTime.now()) {
        val workManager = WorkManager.getInstance(context)
        if (!alerts.weeklyEnabled) {
            workManager.cancelUniqueWork(WEEKLY_WORK_NAME)
            return
        }
        val delay = untilNextWeeklyRun(alerts, from)
        workManager.enqueueUniqueWork(
            WEEKLY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WeeklyForecastWorker>()
                .setInitialDelay(delay)
                .setConstraints(networkRequired)
                .build()
        )
    }

    /** Next occurrence of the configured time of day, today if it has not passed yet. */
    fun untilNextDailyRun(alerts: AlertSettings, from: LocalDateTime): Duration {
        val time = LocalTime.of(alerts.dailyHour, alerts.dailyMinute)
        var next = LocalDateTime.of(from.toLocalDate(), time)
        if (!next.isAfter(from)) next = next.plusDays(1)
        return Duration.between(from, next)
    }

    /** Next occurrence of the configured weekday and time, this week if it is still ahead. */
    fun untilNextWeeklyRun(alerts: AlertSettings, from: LocalDateTime): Duration {
        val target = DayOfWeek.of(alerts.weeklyDayOfWeek)
        val time = LocalTime.of(alerts.weeklyHour, alerts.weeklyMinute)
        val daysAhead = ((target.value - from.dayOfWeek.value) + 7) % 7
        var next = LocalDateTime.of(from.toLocalDate().plusDays(daysAhead.toLong()), time)
        if (!next.isAfter(from)) {
            next = LocalDateTime.of(next.toLocalDate().plusWeeks(1), time)
        }
        return Duration.between(from, next)
    }
}

/** Sunday-first-in-Spanish weekday names, indexed by [DayOfWeek.getValue]. */
fun dayOfWeekLabel(isoDay: Int): String = when (isoDay) {
    1 -> "Lunes"
    2 -> "Martes"
    3 -> "Miércoles"
    4 -> "Jueves"
    5 -> "Viernes"
    6 -> "Sábado"
    else -> "Domingo"
}

/** Short label used in the week strip, e.g. "Hoy", "Mañana", "Vie". */
fun shortDayLabel(date: String, today: LocalDate = LocalDate.now()): String = runCatching {
    when (val parsed = LocalDate.parse(date)) {
        today -> "Hoy"
        today.plusDays(1) -> "Mañana"
        else -> dayOfWeekLabel(parsed.dayOfWeek.value).take(3)
    }
}.getOrElse { date.takeLast(5) }
