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
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.CreateTopicRequest
import com.amazonaws.services.sns.model.PublishRequest
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.messaging.FirebaseMessaging
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

    private var isOverSpeedLimit = false

    private lateinit var snsClient: AmazonSNSClient
    private lateinit var snsTopicArn: String
    private var isAwsInitialized = false

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
        CoroutineScope(Dispatchers.IO).launch {
            initAwsSns()
        }
        registerCarPropertyCallback()
        createNotificationChannel()

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

    private fun initAwsSns() {
        try {
            val credentials = BasicAWSCredentials(
                "AKIA4WJPWR3Y6UKLQVNJ", "LHLZOrjIt222VSkwZmnoVq31hXXfS/tiC2BLxojn"
            )
            snsClient = AmazonSNSClient(credentials)
            snsClient.setRegion(Region.getRegion(Regions.fromName("eu-north-1")))

            snsTopicArn = "arn:aws:sns:eu-north-1:872515276529:fleet_notifications"
            isAwsInitialized = true
            Log.d("AWS-SNS", "Using known SNS topic ARN: $snsTopicArn")
        } catch (e: Exception) {
            Log.e("AWS-SNS", "Failed to initialize SNS client", e)
            isAwsInitialized = false
        }
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

            if (currentSpeed > speedLimit && !isOverSpeedLimit) {
                isOverSpeedLimit = true
                showSpeedingNotification(currentSpeed)
                sendFleetNotification(currentSpeed)
                if (isAwsInitialized) {
                    sendAwsSnsNotification(currentSpeed)
                }
            } else if (currentSpeed <= (speedLimit - 2) && isOverSpeedLimit) {
                isOverSpeedLimit = false
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
            sendFCMNotificationViaHttp(notificationData)
        } catch (e: Exception) {
            Log.e("Firebase", "Error sending fleet notification", e)
        }
    }

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
  "private_key_id": "892212b093771a2e14beb28d8f63fd848ec73d4d",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCA3aMRFcRICKeF\nOVToTJUyLSs1lhZCs6t8WUnWr/kh+5m0aBpjCNXmBPNRC3CYgPZB9pJzAB14FkHR\nLdGyhPHOFunsL3qYzwrYO4fsYQyhF4QlVQfyTBa5j7C1JTL2ER/pYsCD5D7mV4ez\n21nagK+x5xfBUSdSapkAJfrB9B+qB3N5iPBphv15XiNAxU0QdjRwulizi8vXcDyF\nh8gKr2LoCv0mPrnsw5vMbreP6OLApycpjdyKiIahY3tZkN2ymVepodXXP1dqkEJk\na0jWHromGSnKzhoNwqXRzPOu2733rHIDfLaOa3MCCEg83603qnZ6ITIjlGvWUagP\n4nhSYlOhAgMBAAECggEAPwZnDOGU7FgTOlSWYsClzXMK6XvcqkW+TqRKuS26jnTD\nsVii4yG8n3F3YQFT0bps9kj2SjlZsFROX1Rl9UYRDybkxGdaMec82hGPgjva1eSu\n9CU4rDere5i7p3SojnFEprvuBPN6EeToUT4sHXIHu94Wn60lqqjI6KoqkHZpkBnA\nJWvjoBWwl6uciZxk/k9yfZCH3+oLjCZmuePCDj3pKustzu/O5FE2Llw8dq8MEpCw\nMabK9x1SLd3Lw0rQkeWOG1B5bs5quYW/wwnKQ3tXjRXHjVhQqYVys2tHQelwIz5Z\n0v+X8U+KvamqSh4OsdZ3zhuo16Vc2sHEkgusRB18mwKBgQC2NhLtjSjlgCWRpWc5\nIlW9dhsL0zTPUOV3HjCd4bO/oUioJyidW1iP7/GrWvddzQMCy2cN0IsFHE8nk5sX\nVhbI2KHxmqH0/I6/9AOfhSaXaIBV0GwTCQcu9VyYa9O9YYQ2ItlTJZ397D6UFUDa\nrUDUnetuQ0KRtMqItTSmSmh7gwKBgQC1DTa056iT+aaQCimKCez5joHpujkUbcRi\ntQ+keK85UMu+JaFVIl0PsL+3OQ59YRZolMYqRu3B5pVpEdO0L3yitG8NG4OokCl9\nWmaOib1CkaiOoHizwJ16zsP3Rbmrn96L1uozW2OBvBZ7bfhffYIsw4G/tm9PzhFZ\nXVIZtQ7XCwKBgQCWIHV17kFM3kmV9+7Vfzmfkaj1FD7FtYRqoaAy5RyBfjRr/1+S\nTwnoBi1bHucDDQA3FDnDgxiXzFxXYmF9V7wMHZlIhDx+qnw/IaC+lSlXxI1Apyn0\ngRK/GQACwYzC047oP8xJmovTBAwHXH+D7Q2mYLryrU4y2P+qpYpMnK/7WwKBgD+C\nVBdfT74gi7HpLqsSUkM0HxQq08z7uDDbzJHoF6cmVWHs90vS0wwm9wlBhXirrt2e\nmmjIGqyywuRpcXa6VaEJZA8YALYHY9Zp+KG1ZWNNjvABEHYVcehbUViS589xM/Kf\nv+WmACDJJqXDbKWqdBhZuFDUoPVAlstfNeZ4oa0DAoGBAJP3pqRdtFe5l+xS96/Z\nLhLE+PRQTSwlSYtkju63h1Qy/APpl6Vve5crvOWpTc3Tx+FwRGzKk/L/jRp8FUmi\nwA2JTn9a/9Xns15t740tGrqsq7t4NSlKo0KZLBQ3ian3CCz41k0yRztrkEh9ZFPK\nOEAKkayeyBj+f+rWwpDIt9sv\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-fbsvc@aaos-assignment.iam.gserviceaccount.com",
  "client_id": "115002391646449095117",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40aaos-assignment.iam.gserviceaccount.com",
  "universe_domain": "googleapis.com"
}
    """.trimIndent()
        val credentials = GoogleCredentials.fromStream(serviceAccountJson.byteInputStream())
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

    private fun sendAwsSnsNotification(currentSpeed: Float) {
        if (!isAwsInitialized || !::snsTopicArn.isInitialized) {
            Log.e("AWS-SNS", "Cannot send notification - AWS not initialized")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = JSONObject().apply {
                    put("driver", name)
                    put("currentSpeed", currentSpeed.toString())
                    put("speedLimit", speedLimit.toString())
                }.toString()

                val publishRequest = PublishRequest().withTopicArn(snsTopicArn).withMessage(message)
                    .withSubject("Speed Alert: Driver $name Exceeding Limit")

                val publishResult = snsClient.publish(publishRequest)
                Log.d("AWS-SNS", "Message sent with ID: ${publishResult.messageId}")
            } catch (e: Exception) {
                Log.e("AWS-SNS", "Failed to send SNS notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCarPropertyManager.unregisterCallback(speedCallback)
        car.disconnect()
        firebaseMessaging.unsubscribeFromTopic(FLEET_TOPIC)
    }
}