package com.app.fleetapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data?.let { data ->
            val title = data["title"] ?: "Speed Alert 🚗"
            val body = data["body"] ?: "Speed limit exceeded!"
            val speedLimit = data["limit"] ?: "Unknown"
            val name = data["name"] ?: "Unknown Driver"

            showNotification(title, body, speedLimit, name)
        }
    }

    private fun showNotification(title: String, body: String, limit: String, name: String) {
        val channelId = "speed_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Speed Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when speed limit is exceeded"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText("$body\nLimit: $limit km/h\nDriver: $name")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(9999, notification)
    }
}