package com.smartexambox.security.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartexambox.security.MainActivity
import com.smartexambox.security.R
import com.smartexambox.security.data.model.SeverityLevel

class SecurityNotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL,
                "Critical Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate alerts for geofence breaches, lockouts, and unauthorized access"
                enableVibration(true)
                enableLights(true)
            }

            val eventsChannel = NotificationChannel(
                CHANNEL_EVENTS,
                "Security Status & Vault Events",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General security audit and vault access notifications"
            }

            notificationManager.createNotificationChannel(criticalChannel)
            notificationManager.createNotificationChannel(eventsChannel)
        }
    }

    fun sendSecurityAlertNotification(
        title: String,
        message: String,
        severity: SeverityLevel
    ) {
        val channelId = if (severity == SeverityLevel.CRITICAL || severity == SeverityLevel.HIGH) {
            CHANNEL_CRITICAL
        } else {
            CHANNEL_EVENTS
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_security_shield)
            .setContentTitle("[$severity] $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(
                if (severity == SeverityLevel.CRITICAL) NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Notification permission might be restricted on Android 13+
        }
    }

    companion object {
        const val CHANNEL_CRITICAL = "channel_critical_alerts"
        const val CHANNEL_EVENTS = "channel_security_events"

        @Volatile
        private var instance: SecurityNotificationHelper? = null

        fun getInstance(context: Context): SecurityNotificationHelper {
            return instance ?: synchronized(this) {
                instance ?: SecurityNotificationHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
