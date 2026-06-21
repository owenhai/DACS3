package com.example.dacs3.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dacs3.R
import com.example.dacs3.databinding.ViewholderAdminFoodBinding
import com.example.dacs3.model.FoodItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AdminFoodAdapter(
    private val foods: List<FoodItem>,
    private val onEditClick: (FoodItem) -> Unit,
    private val onDeleteClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<AdminFoodAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewholderAdminFoodBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderAdminFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foods[position]
        holder.binding.foodNameTxt.text = food.name
        holder.binding.foodPriceTxt.text = formatVnd(food.price)

        Glide.with(holder.itemView.context)
            .load(food.imagePath)
            .placeholder(R.drawable.blur_bg)
            .into(holder.binding.foodImg)

        holder.binding.editBtn.setOnClickListener { onEditClick(food) }
        holder.binding.deleteBtn.setOnClickListener { onDeleteClick(food) }
    }

    override fun getItemCount(): Int = foods.size

    private fun formatVnd(amount: Double): String {
        val formatter = DecimalFormat("#,###")
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        formatter.decimalFormatSymbols = symbols
        return "${formatter.format(amount)} đ"
    }
}
