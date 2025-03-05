package com.app.drivealert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.notification.CarAppExtender
import androidx.car.app.validation.HostValidator
import androidx.core.app.NotificationCompat
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SpeedService : CarAppService() {
    private val channelId = "speeding_channel"
    private lateinit var car: Car
    private lateinit var mCarPropertyManager: CarPropertyManager
    private var speedLimit: Int = 140
    private var name: String = "140"
    private val firebaseMessaging = FirebaseMessaging.getInstance()

    companion object {
        private const val KM_MULTIPLIER = 3.59999987F // Use to convert miles to kms
        private const val notificationId = 1234
        private const val FLEET_TOPIC = "/topics/fleet_notifications"
    }

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        speedLimit = intent?.getIntExtra("limit", 150) ?: 150
        name = intent?.getStringExtra("name") ?: ""
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        initCar()
        registerCarPropertyCallback()
        createNotificationChannel()

        // Subscribe to fleet notifications topic
        firebaseMessaging.subscribeToTopic(FLEET_TOPIC).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firebase", "Subscribed to fleet notifications")
            } else {
                Log.e("Firebase", "Subscription failed", task.exception)
            }
        }
    }

    private fun initCar() {
        car = Car.createCar(applicationContext)
        mCarPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
    }

    private fun registerCarPropertyCallback() {
        mCarPropertyManager.registerCallback(
            speedCallback,
            VehiclePropertyIds.PERF_VEHICLE_SPEED,
            CarPropertyManager.SENSOR_RATE_FASTEST
        )
    }

    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            val currentSpeed = ((carPropertyValue.value as Float) * KM_MULTIPLIER)

            if (currentSpeed > speedLimit) {
                showSpeedingNotification(currentSpeed)
                sendFleetNotification(currentSpeed)
            }
        }

        override fun onErrorEvent(i: Int, i1: Int) {
            Log.e(ContentValues.TAG, "CarPropertyManager Error!")
        }
    }

    private fun sendFleetNotification(currentSpeed: Float) {
        try {
            val notificationData = JSONObject().apply {
                put("speed", currentSpeed.toString())
                put("speedLimit", speedLimit.toString())
                put("driverName", name)
            }

            // Send notification via Firebase Cloud Messaging
            val message = RemoteMessage.Builder(FLEET_TOPIC).addData("type", "speed_alert")
                .addData("details", notificationData.toString()).build()

            // Optional: Send via HTTP request for more control
            sendFCMNotificationViaHttp(notificationData)

        } catch (e: Exception) {
            Log.e("Firebase", "Error sending fleet notification", e)
        }
    }

    // Replace sendFCMNotificationViaHttp with this
    private fun sendFCMNotificationViaHttp(notificationData: JSONObject) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val accessToken = getFcmAccessToken()

                val url =
                    URL("https://fcm.googleapis.com/v1/projects/aaos-assignment/messages:send")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"

                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val payload = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("topic", "fleet_notifications") // Remove /topics/ prefix
                        put("data", notificationData)
                    })
                }
                connection.outputStream.use { os ->
                    os.write(payload.toString().toByteArray())
                }
                when (connection.responseCode) {
                    200 -> Log.d("FCM", "Notification sent successfully")
                    else -> Log.e("FCM", "Error: ${connection.responseMessage}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error sending notification", e)
            }
        }
    }

    private suspend fun getFcmAccessToken(): String {
        val serviceAccountJson = """
    {
        "type": "service_account",
        "project_id": "aaos-assignment",
        "private_key_id": "2be1de406bb228022109a4ff0479b04751950120",
        "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCcuSXbdQR7YjNl\naNBKJ+4b3RKv0PxQ1TM//1YBLrxJBhw3BdoCvlz8I2TKN23tcdlrDAQv08tx8mFj\nPmqk1oyI00JnXkfzTcclplloDJJWojkRhJqyvgcW4oUGNzJoPSm7Yt2Ibp3ScIWk\nfVePBSt2p0TrnVlNzLERxMJA1XB9iiIwx0Vn+vZv/ePh2/aG769q+wrYTp19Nbit\nkJcGfqGW38YcFz7M9tPVd04O7dyZse4C+TLnd8eYCglpNNjeECjSs1qTIKqJBlsb\nnDmdWaH86YFhOLnpKYmrXW5Bug9d481V7JrUj9AvHNKS5JN/Ulz9qYk/IuLQz/LY\nm2WIIodnAgMBAAECggEACwPfi329ZrQ8a1U8xoMcWV/MldTfcKL7d8HV5Qr7qtPZ\nUDi2EPLmQCzddnHQGE89k8HqQJ5NmODYEKY5z2odwh273hE8IPdt454kf76nQZvw\n3OO82vE/0ejyQCfJZpaRHbceKoRzzTwHxWVJFFrg3VvctgUYjYvkdulx9owwQtjb\nuQBPK5rwLy2c85nIfg3ycVib/YrMYjKsjnl07vdXryQaGKRMLZ+y+bUbBKDwdGKf\nESPxyLtKQGHO/PmSTzwoFMui1MQDSwQIjoGpfesIgP6YBghtdRfDm4Z1keBZvPg9\njPEk3yV83rDo+TwBZ4CJtIR9tWXO0PLJJGJofoZkmQKBgQDafnKk0PG3ys1RfSRb\nmcau8SYvstU1XJySAwYK2akO/wCPRoWcQJbvB6yjMX6NRC8f/MhDWh+Oouc71gP2\nOB2R94b6B5QdgCFY7gSXc1U6cDkCNSBawlcbY19aPJXTWHMnRAg8zJQSNO83wRa7\nvwYumn9Slg7EvaDamt85lC+cFQKBgQC3oDubpfr5GbxDfsOr4BjpkqkWP/w3r7e1\nv0VR8KPOUrOEbFmwlJWoxNmwRU+jKOwy1+2RthQhUnU8pDjZtgcrLBwMC5uTZCU5\nWPEI/omnwxJpavYb3PVn0XQeeOFL4FZ8xA5wREDsfEkffyaWHpqzSrBadVoWpvmU\ncmcffC+oiwKBgQCbEd1p7kVvzQaVjDyuqmAhr1MLzV+WxMj2tCCouLxMwVzzUvjm\n4cx9jzPOiwUnW7oaeLzlWGRHRoTYqdcb+8242DW65IARw+tnA17M4nUdyaIcV/pD\nj+Rkss1s7MzlN8RanOU/yW5Z0anqJydsTnIjx1EQGyIWLgOowvwlEHAN1QKBgQCL\nGrBtTgTdFhjfjLVQjBhzQS4kc95ulQf3f+lUlJaS42WpCKe9NCJX0qASf78/m+2q\n6pbHDocbYyr7AHEsxE+T7ftlSJnXw0LLucwHA0RmuUlGYMgF/ZJOzoZH20zZPR5n\nr+I8PAhu3gxK9vjVOmP5NLh6HnJbvKxPP4dNe7di1wKBgCP1lqYyaAu8FLf2iHep\nvpHeZ1cjBcNqStGXjPouGFi0lMvT7LIegNyoisHnpOgamkghkBOxtwzDmFsmyA9g\nlEoFxE3N0CUd1TsRn6IhK56ThvRi9w7FgP+JpLW9fA33AdyNY/x7frigauN2Qod4\nmwcfLoq6aHtDpBNErmFCLRWw\n-----END PRIVATE KEY-----\n",
        "client_email": "firebase-adminsdk-fbsvc@aaos-assignment.iam.gserviceaccount.com",
        "client_id": "115002391646449095117",
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "token_uri": "https://oauth2.googleapis.com/token",
        "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
        "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40aaos-assignment.iam.gserviceaccount.com",
        "universe_domain": "googleapis.com"
    }
    """.trimIndent()
        val credentials =
            GoogleCredentials.fromStream(serviceAccountJson.byteInputStream())
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))

        return credentials.refreshAccessToken().tokenValue
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Speeding Channel"
            val descriptionText = "Speeding notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                enableVibration(true)
                vibrationPattern =
                    longArrayOf(0, 250, 250, 250) // Vibration pattern for heads-up notification
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showSpeedingNotification(currentSpeed: Float) {
        val notificationIntent = Intent(this, SpeedService::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Need for Speed? Not Today! 😅")
            .setContentText("Whoa! Your speed is $currentSpeed km/h. Let's keep it safe! 🛑")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL).setAutoCancel(true).extend(
                CarAppExtender.Builder().setContentTitle("Car Speed Alert!").build()
            ).setFullScreenIntent(pendingIntent, true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        mCarPropertyManager.unregisterCallback(speedCallback)
        car.disconnect()
        firebaseMessaging.unsubscribeFromTopic(FLEET_TOPIC)
    }
}