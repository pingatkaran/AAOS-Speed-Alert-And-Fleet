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
import java.io.BufferedReader
import java.io.InputStreamReader
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
        private const val FCM_SERVER_KEY =
            "ya29.c.c0ASRK0Ga0_SVrxWfnl27tvblxZIyI_R0nYCvURKzK9FXjI0itped3s8uEbIvm-K6kqA21vl327l_e8u-y-y9GiUXcbxUw_fRYN_xxV-jfwGg3kqj6ny8hiCE3pV7WVIP3WtqIYEEwqtmJ3O_oRJUU9MfcNVygPcokUSlh0e-svEGD6QH1IZyF20gz-KV8ObSparD6LnRCf9Oamk2n31HzLD2RhasVGF3DVI2VWLe0QOhLCqiH2zoNIvnUH-sjJ5Wr0wvvlyMfDPZbXXJ5WkgeROwE46q1FbxAPfQKfMEP87w_6qaqZqTvQLmhPzF7Xb7zo3l88huwU_42uv1d-Fseca-hdm9CPUbDtRX0KusbWox0rxmA5oCIgdtzG385P7i6e2J5y-YQmetzq9qjFFWp1x20R8mbYOm3fc7uMWt-Xl-4-M_ntMjgBjfaXi7OkUivJvR31V5s9iSfZBqX0S4qJdriJ45xp206kWilwv-oF608XSXz5ZiMrhlY9jYdcRzqc9tY-b_y3JQ_fXWcnRmOfM6uMncpQ6cQ_S_yj5Xhna6htIfjUUXs0s4jYynM096WhpcixyY9jiX8yq2qegmXpets4_kWRvxwzjdBlSjctix3IJ1r-qpcx0ZxWJaXi140h_heV68dtmzRmSMhcFI7wWaQ3JO2O1B017WQmQrBfrROlfb0_vfzV8d-XISgwMyFxoYIiZwMj9X4iS9XFeJFae5bc-UWBd4MSZbXoMbU8_S2svZ70hphXZZfx4jtlecpORcup6RmVzwr6VYv7kaUtt2IZiuZZ9a8BXMiiXzM4wMr9Qlq7_Rk0h2hXwiMlJ2Jnaj4es1lfpywYcRvz8MJvIOtfX0U90kRSflYx-v9rFMO24auF4mazFkV1Vui6-0YQi5I01gJzlfrnbF0RXaxyqxZfvmR0QJ4Q0_8msv888W1--WuXsJ4v-j909vcxrxO0UpsFk1tWorBjFtfst6dh9cVIn9FZ4zovQjJ3iXvtZwWm-U8110QVu6" // Replace with your actual server key
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
        firebaseMessaging.subscribeToTopic(FLEET_TOPIC)
            .addOnCompleteListener { task ->
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
            // Prepare notification data
            val notificationData = JSONObject().apply {
                put("speed", currentSpeed.toString())
                put("speedLimit", speedLimit.toString())
                put("driverName", name)
            }

            // Send notification via Firebase Cloud Messaging
            val message = RemoteMessage.Builder(FLEET_TOPIC)
                .addData("type", "speed_alert")
                .addData("details", notificationData.toString())
                .build()

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
                // 1. Get access token
                val accessToken = getFcmAccessToken()

                // 2. Create proper FCM v1 request
                val url = URL("https://fcm.googleapis.com/v1/projects/aaos-assignment/messages:send")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"

                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // 3. Build v1-compatible payload
                val payload = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("topic", "fleet_notifications") // Remove /topics/ prefix
                        put("data", notificationData)
                    })
                }

                Log.e("TAG", "payload: ${payload}", )
                Log.e("TAG", "accessToken: ${accessToken}", )

                // 4. Send request
                connection.outputStream.use { os ->
                    os.write(payload.toString().toByteArray())
                }

                // 5. Check response
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
        // Get from secure storage (DO NOT HARDCODE)
        val credentials = GoogleCredentials.fromStream(applicationContext.assets.open("service_account.json"))
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
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .extend(
                CarAppExtender.Builder()
                    .setContentTitle("Car Speed Alert!")
                    .build()
            )
            .setFullScreenIntent(pendingIntent, true)

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