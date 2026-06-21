package com.example.dacs3.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.R
import com.example.dacs3.databinding.ItemTimeBinding

class TimeAdapter(private val timeSlots : List<String>, private val onTimeSelected: (String) -> Unit) :
    RecyclerView.Adapter<TimeAdapter.ViewHolder>() {
    private var selectedPosition = 0
    private var lastselectedPosition = -1

    fun getSelectedTime(): String? {
        return if (selectedPosition != -1) timeSlots[selectedPosition] else null
    }

    inner class ViewHolder(private val binding : ItemTimeBinding) :
    RecyclerView.ViewHolder(binding.root){
        fun bind(time : String ){

                binding.TextViewTime.text = time

                if(selectedPosition == position){
                    binding.TextViewTime.setBackgroundResource(R.drawable.yellow_bg)
                    binding.TextViewTime.setTextColor(binding.root.context.getColor(R.color.black))

                }else{
                    binding.TextViewTime.setBackgroundResource(R.drawable.light_black_bg)
                    binding.TextViewTime.setTextColor(binding.root.context.getColor(R.color.white))
                }

                binding.root.setOnClickListener {
                    val position = adapterPosition
                    if(position != RecyclerView.NO_POSITION){
                        lastselectedPosition = selectedPosition
                        selectedPosition = position
                        notifyItemChanged(lastselectedPosition)
                        notifyItemChanged(selectedPosition)
                        onTimeSelected(time)
                    }
                }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TimeAdapter.ViewHolder {
        return ViewHolder(
            ItemTimeBinding.inflate(
            LayoutInflater.from(parent.context), parent,
            false
         )
        )
    }

    override fun onBindViewHolder(holder: TimeAdapter.ViewHolder, position: Int) {
        holder.bind(timeSlots[position])
    }

    override fun getItemCount(): Int = timeSlots.size

}