package com.app.fleetapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddCar: FloatingActionButton
    private val carAdapter = CarAdapter()

    private val database = FirebaseDatabase.getInstance()
    private val carsRef = database.getReference("cars")
    private lateinit var fcmTokenHelper: FcmTokenHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        setupRecyclerView()
        setupListeners()
        loadCars()
        fcmTokenHelper = FcmTokenHelper(this)

        FirebaseMessaging.getInstance().subscribeToTopic("speed_alert")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to Speed Alert topic")
                } else {
                    Log.e("FCM", "Subscription failed")
                }
            }
        fcmTokenHelper.generateAccessToken(
            onSuccess = { token ->
                Log.d("FCM", "Access Token: $token")
            },
            onError = { error ->
                Log.e("FCM", "Error: ${error.message}")
            }
        )
    }


    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        fabAddCar = findViewById(R.id.fabAddCar)
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = carAdapter
        }
    }

    private fun setupListeners() {
        fabAddCar.setOnClickListener {
            startActivity(Intent(this, AddCarActivity::class.java))
        }
    }

    private fun loadCars() {
        carsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val carsList = mutableListOf<Car>()
                for (carSnapshot in snapshot.children) {
                    carSnapshot.getValue(Car::class.java)?.let { car ->
                        carsList.add(car)
                    }
                }
                carAdapter.updateCars(carsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load cars", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        fcmTokenHelper.clear() // Cleanup coroutines
    }
}
