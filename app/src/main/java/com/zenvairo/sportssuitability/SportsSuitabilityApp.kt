package com.zenvairo.sportssuitability

import android.app.Application
import com.zenvairo.sportssuitability.alerts.AlertScheduler
import com.zenvairo.sportssuitability.alerts.ensureNotificationChannels
import com.zenvairo.sportssuitability.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SportsSuitabilityApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels(this)
        // Re-arm on every cold start: the workers survive reboots via WorkManager, but this
        // also picks up any schedule the user changed while the app was not running.
        appScope.launch {
            val alerts = SettingsRepository(this@SportsSuitabilityApp).settings.first().alerts
            AlertScheduler.rescheduleAll(this@SportsSuitabilityApp, alerts)
        }
    }
}
