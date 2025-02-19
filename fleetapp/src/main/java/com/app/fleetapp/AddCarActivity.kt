package com.app.fleetapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

class AddCarActivity : AppCompatActivity() {

    private lateinit var etCarModel: EditText
    private lateinit var etDriverName: EditText
    private lateinit var etSpeedLimit: EditText
    private lateinit var btnSave: Button

    private val database = FirebaseDatabase.getInstance()
    private val carsRef = database.getReference("cars")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        etCarModel = findViewById(R.id.etCarModel)
        etDriverName = findViewById(R.id.etDriverName)
        etSpeedLimit = findViewById(R.id.etSpeedLimit)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveCar()
        }
    }

    private fun saveCar() {
        val carModel = etCarModel.text.toString().trim()
        val driverName = etDriverName.text.toString().trim()
        val speedLimitStr = etSpeedLimit.text.toString().trim()

        if (carModel.isEmpty() || driverName.isEmpty() || speedLimitStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val speedLimit = speedLimitStr.toIntOrNull()
        if (speedLimit == null) {
            Toast.makeText(this, "Please enter a valid speed limit", Toast.LENGTH_SHORT).show()
            return
        }

        val carId = UUID.randomUUID().toString()
        val car = Car(carId, carModel, driverName, speedLimit)

        carsRef.child(carId).setValue(car)
            .addOnSuccessListener {
                Toast.makeText(this, "Car data saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save car data", Toast.LENGTH_SHORT).show()
            }
    }
}