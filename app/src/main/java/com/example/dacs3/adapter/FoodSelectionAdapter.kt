package com.example.dacs3.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dacs3.R
import com.example.dacs3.databinding.ViewholderFoodSelectionBinding
import com.example.dacs3.model.FoodItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class FoodSelectionAdapter(
    private val foods: List<FoodItem>,
    private val onQuantityChanged: (Map<String, Int>) -> Unit
) : RecyclerView.Adapter<FoodSelectionAdapter.ViewHolder>() {

    private val quantities = mutableMapOf<String, Int>()

    class ViewHolder(val binding: ViewholderFoodSelectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderFoodSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food = foods[position]
        holder.binding.foodNameTxt.text = food.name
        holder.binding.foodPriceTxt.text = formatVnd(food.price)
        
        val qty = quantities[food.id] ?: 0
        holder.binding.quantityTxt.text = qty.toString()

        Glide.with(holder.itemView.context)
            .load(food.imagePath)
            .placeholder(R.drawable.blur_bg)
            .into(holder.binding.foodImg)

        holder.binding.plusBtn.setOnClickListener {
            val currentQty = quantities[food.id] ?: 0
            quantities[food.id] = currentQty + 1
            notifyItemChanged(position)
            onQuantityChanged(quantities)
        }

        holder.binding.minusBtn.setOnClickListener {
            val currentQty = quantities[food.id] ?: 0
            if (currentQty > 0) {
                quantities[food.id] = currentQty - 1
                notifyItemChanged(position)
                onQuantityChanged(quantities)
            }
        }
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
