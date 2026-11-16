package com.zenvairo.sportssuitability.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zenvairo.sportssuitability.MainActivity

const val DAILY_CHANNEL_ID = "daily_suitability_alerts"
const val WEEKLY_CHANNEL_ID = "weekly_forecast_alerts"

private const val NOTIFICATION_GROUP = "com.zenvairo.sportssuitability.CONDITIONS"

fun ensureNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
        NotificationChannel(
            DAILY_CHANNEL_ID,
            "Aviso del día",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Te dice cada mañana si hoy puedes ir a cada sitio guardado" }
    )
    manager.createNotificationChannel(
        NotificationChannel(
            WEEKLY_CHANNEL_ID,
            "Previsión semanal",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Resumen de la semana que viene, con los mejores días de cada sitio" }
    )
}

private fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

private fun openAppIntent(context: Context, requestCode: Int): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

/**
 * Posts one notification, expanding to [body] when the user pulls it down.
 * Silently no-ops when the user has not granted the notification permission.
 */
fun postNotification(
    context: Context,
    channelId: String,
    notificationId: Int,
    title: String,
    summary: String,
    body: String = summary
) {
    if (!canPostNotifications(context)) return
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(summary)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup(NOTIFICATION_GROUP)
        .setContentIntent(openAppIntent(context, notificationId))
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(notificationId, notification)
}
