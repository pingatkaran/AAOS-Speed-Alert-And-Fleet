package com.app.drivealert

import android.Manifest
import android.car.Car
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var limitEditText: EditText
    private lateinit var startButton: Button
    private lateinit var messageTextView: TextView
    private lateinit var layoutContainer: LinearLayout

    object Constant {
        val permissions = arrayOf(
            Car.PERMISSION_SPEED,
            Car.PERMISSION_CAR_INFO,
            Manifest.permission.USE_FULL_SCREEN_INTENT,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        layoutContainer = findViewById(R.id.layoutContainer)
        nameEditText = findViewById(R.id.nameEditText)
        limitEditText = findViewById(R.id.limitEditText)
        startButton = findViewById(R.id.startButton)
        messageTextView = findViewById(R.id.messageTextView)

        startButton.setOnClickListener {
            checkPermissionsAndStartService()
        }
    }

    private fun checkPermissionsAndStartService() {
        val allGranted = Constant.permissions.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startSpeedService()
        } else {
            ActivityCompat.requestPermissions(this, Constant.permissions, 0)
        }
    }

    private fun startSpeedService() {
        val name = nameEditText.text.toString().trim()
        val limit = limitEditText.text.toString().trim()

        if (name.isEmpty() || limit.isEmpty()) {
            Toast.makeText(this, "Please enter both name and limit", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, SpeedService::class.java).apply {
            putExtra("name", name)
            putExtra("limit", limit.toInt())
        }
        startService(intent)

        // Hide input fields and button, then show message
        layoutContainer.visibility = View.GONE
        messageTextView.text = "Hi $name, Please drive below $limit kmph"
        messageTextView.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (allGranted) {
            startSpeedService()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}