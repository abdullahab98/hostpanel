package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "hostpanel_alerts"
        const val CHANNEL_NAME = "HostPanel Server Alerts"
        private var notificationIdCounter = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for process crashes, restarts, and critical system events"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showServerAlert(title: String, message: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationIdCounter++, builder.build())
            }
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission on Android 13+, ignore gracefully
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
