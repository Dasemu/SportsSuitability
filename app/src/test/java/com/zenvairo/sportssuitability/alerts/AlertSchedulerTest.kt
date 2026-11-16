package com.zenvairo.sportssuitability.alerts

import com.zenvairo.sportssuitability.data.prefs.AlertSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class AlertSchedulerTest {

    private val alerts = AlertSettings(
        dailyHour = 7, dailyMinute = 0,
        weeklyDayOfWeek = 7, weeklyHour = 18, weeklyMinute = 0
    )

    // 2026-09-04 is a Friday.
    private fun friday(hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 9, 4, hour, minute)

    @Test
    fun `daily alert fires later today when the hour has not passed`() {
        assertEquals(Duration.ofHours(2), AlertScheduler.untilNextDailyRun(alerts, friday(5)))
    }

    @Test
    fun `daily alert rolls over to tomorrow once the hour has passed`() {
        assertEquals(Duration.ofHours(14), AlertScheduler.untilNextDailyRun(alerts, friday(17)))
    }

    @Test
    fun `daily alert scheduled exactly on the hour waits a full day`() {
        assertEquals(Duration.ofHours(24), AlertScheduler.untilNextDailyRun(alerts, friday(7)))
    }

    @Test
    fun `weekly alert lands on the coming Sunday evening`() {
        // Friday 10:00 → Sunday 18:00 is 2 days and 8 hours away.
        assertEquals(
            Duration.ofDays(2).plusHours(8),
            AlertScheduler.untilNextWeeklyRun(alerts, friday(10))
        )
    }

    @Test
    fun `on the day itself it fires the same evening`() {
        val sundayNoon = LocalDateTime.of(2026, 9, 6, 12, 0)
        assertEquals(Duration.ofHours(6), AlertScheduler.untilNextWeeklyRun(alerts, sundayNoon))
    }

    @Test
    fun `once Sunday evening has passed it waits a full week`() {
        val sundayNight = LocalDateTime.of(2026, 9, 6, 20, 0)
        assertEquals(
            Duration.ofDays(6).plusHours(22),
            AlertScheduler.untilNextWeeklyRun(alerts, sundayNight)
        )
    }

    @Test
    fun `a different configured weekday is honoured`() {
        val wednesdayAlerts = alerts.copy(weeklyDayOfWeek = 3, weeklyHour = 9, weeklyMinute = 30)
        // Friday 10:00 → next Wednesday 09:30 is 4 days 23:30 away.
        assertEquals(
            Duration.ofDays(4).plusHours(23).plusMinutes(30),
            AlertScheduler.untilNextWeeklyRun(wednesdayAlerts, friday(10))
        )
    }
}
