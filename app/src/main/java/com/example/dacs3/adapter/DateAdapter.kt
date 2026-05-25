package com.example.dacs3.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.R
import com.example.dacs3.databinding.ItemDateBinding
import kotlin.time.TimedValue

class DateAdapter(private val timeSlots : List<String>, private val onDateSelected: (String) -> Unit) :
    RecyclerView.Adapter<DateAdapter.ViewHolder>() {
    private var selectedPosition = 0
    private var lastselectedPosition = -1

    fun getSelectedDate(): String? {
        return if (selectedPosition != -1) timeSlots[selectedPosition] else null
    }

    inner class ViewHolder(private val binding : ItemDateBinding) :
    RecyclerView.ViewHolder(binding.root){
        fun bind(date : String ){
            val dateParts = date.split("/")
            if(dateParts.size == 3)
            {
                binding.dayTxt.text = dateParts[0]
                binding.dayMonthTxt.text = dateParts[1] +  " " + dateParts[2]

                if(selectedPosition == position){
                    binding.mainLayout.setBackgroundResource(R.drawable.orange_bg)
                    binding.dayTxt.setTextColor(binding.root.context.getColor(R.color.black))
                    binding.dayMonthTxt.setTextColor(binding.root.context.getColor(R.color.black))
                }else{
                    binding.mainLayout.setBackgroundResource(R.drawable.light_black_bg)
                    binding.dayTxt.setTextColor(binding.root.context.getColor(R.color.white))
                    binding.dayMonthTxt.setTextColor(binding.root.context.getColor(R.color.white))
                }

                binding.root.setOnClickListener {
                    val position = adapterPosition
                    if(position != RecyclerView.NO_POSITION){
                        lastselectedPosition = selectedPosition
                        selectedPosition = position
                        notifyItemChanged(lastselectedPosition)
                        notifyItemChanged(selectedPosition)
                        onDateSelected(date)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DateAdapter.ViewHolder {
        return ViewHolder(
            ItemDateBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent,
            false
         )
        )
    }

    override fun onBindViewHolder(holder: DateAdapter.ViewHolder, position: Int) {
        holder.bind(timeSlots[position])
    }

    override fun getItemCount(): Int = timeSlots.size

}