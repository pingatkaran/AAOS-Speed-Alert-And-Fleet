package com.app.drivealert

import android.Manifest
import android.car.Car
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CarListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageTextView: TextView
    private lateinit var layoutContainer: LinearLayout
    private lateinit var backButton: Button

    private val carAdapter = CarAdapter { selectedCar ->
        checkPermissionsAndStartService(selectedCar)
    }

    private val database = FirebaseDatabase.getInstance()
    private val carsRef = database.getReference("cars")

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
        setContentView(R.layout.activity_car_list)

        initializeViews()
        setupRecyclerView()
        loadCars()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        messageTextView = findViewById(R.id.messageTextView)
        layoutContainer = findViewById(R.id.layoutContainer)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            recyclerView.visibility = View.VISIBLE
            layoutContainer.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CarListActivity)
            adapter = carAdapter
        }
    }

    private fun loadCars() {
        carsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val carsList = mutableListOf<CarData>()
                for (carSnapshot in snapshot.children) {
                    carSnapshot.getValue(CarData::class.java)?.let { car ->
                        carsList.add(car)
                    }
                }
                carAdapter.updateCars(carsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CarListActivity, "Failed to load cars", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkPermissionsAndStartService(car: CarData) {
        val allGranted = Constant.permissions.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startSpeedService(car)
        } else {
            ActivityCompat.requestPermissions(this, Constant.permissions, 0)
        }
    }

    private fun startSpeedService(car: CarData) {
        val intent = Intent(this, SpeedService::class.java).apply {
            putExtra("name", car.driverName)
            putExtra("limit", car.speedLimit)
        }
        startService(intent)

        // Hide RecyclerView and show message with Back Button
        recyclerView.visibility = View.GONE
        layoutContainer.visibility = View.VISIBLE
        messageTextView.text = "Hi ${car.driverName}, Please drive below ${car.speedLimit} kmph"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (allGranted) {
            val selectedCar = carAdapter.getSelectedCar()
            selectedCar?.let { startSpeedService(it) }
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}