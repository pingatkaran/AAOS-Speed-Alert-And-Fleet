package com.app.fleetapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.fleetapp.databinding.ItemCarBinding

class CarAdapter : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {
    private var cars = mutableListOf<Car>()

    class CarViewHolder(private val binding: ItemCarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(car: Car) {
            binding.tvCarModel.text = "Model: ${car.carModel}"
            binding.tvDriverName.text = "Driver: ${car.driverName}"
            binding.tvSpeedLimit.text = "Speed Limit: ${car.speedLimit} km/h"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(cars[position])
    }

    override fun getItemCount() = cars.size

    fun updateCars(newCars: List<Car>) {
        cars.clear()
        cars.addAll(newCars)
        notifyDataSetChanged()
    }
}