package com.app.drivealert

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.drivealert.databinding.ItemCarBinding

class CarAdapter(private val onItemClick: (CarData) -> Unit) :
    RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    private var cars = mutableListOf<CarData>()
    private var selectedCar: CarData? = null

    class CarViewHolder(private val binding: ItemCarBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(car: CarData, onItemClick: (CarData) -> Unit) {
            binding.tvCarModel.text = "Model: ${car.carModel}"
            binding.tvDriverName.text = "Driver: ${car.driverName}"
            binding.tvSpeedLimit.text = "Speed Limit: ${car.speedLimit} km/h"

            binding.root.setOnClickListener {
                onItemClick(car)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(cars[position]) { car ->
            selectedCar = car
            onItemClick(car)
        }
    }

    override fun getItemCount() = cars.size

    fun updateCars(newCars: List<CarData>) {
        cars.clear()
        cars.addAll(newCars)
        notifyDataSetChanged()
    }

    fun getSelectedCar(): CarData? {
        return selectedCar
    }
}