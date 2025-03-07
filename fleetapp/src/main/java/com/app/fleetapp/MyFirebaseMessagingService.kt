package com.app.fleetapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received: ${remoteMessage.data}")

        remoteMessage.data.let { data ->
            data["speed"]?.toDoubleOrNull()?.let { speed ->
                data["speedLimit"]?.toIntOrNull()?.let { limit ->
                    data["driverName"]?.let { driver ->
                        showSpeedAlert(speed, limit, driver)
                    }
                }
            }
        }
    }

    private fun showSpeedAlert(speed: Double, limit: Int, driver: String) {
        val channelId = "fleet_channel"
        createNotificationChannel(channelId)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alert: $driver is on High Speed")
            .setContentText("Current speed: ${speed}km/h (Limit: $limit km/h)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission()
                return
            }
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun requestNotificationPermission() {
        val permissionIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Ensure it starts correctly
        }
        startActivity(permissionIntent)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Fleet Alerts"
            val descriptionText = "Notifications for speed alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        FirebaseMessaging.getInstance().subscribeToTopic("fleet_notifications")
    }
}