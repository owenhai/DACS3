package com.example.dacs3.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.R
import com.example.dacs3.databinding.ItemSeatBinding
import com.example.dacs3.model.Seat

class SeatListAdapter(private val seatList: List<Seat>,
                      private val context : Context,
                      private val selectedSeat : SelectedSeat
    ) :
    RecyclerView.Adapter<SeatListAdapter.ViewHolder>() {
        private val selectedSeatName = ArrayList<String>()
    inner class ViewHolder(val binding : ItemSeatBinding) :
        RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SeatListAdapter.ViewHolder {
        return ViewHolder(ItemSeatBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder : SeatListAdapter.ViewHolder, position : Int) {
        val seat = seatList[position]
        holder.binding.seatTxt.text = seat.name
        when(seat.status){
            Seat.SeatStatus.AVAILABLE -> {
                holder.binding.seatTxt.setBackgroundResource(R.drawable.ic_seat_available)
                holder.binding.seatTxt.setTextColor(context.getColor(R.color.white))
            }
            Seat.SeatStatus.UNAVAILABLE -> {
                holder.binding.seatTxt.setBackgroundResource(R.drawable.ic_seat_unavailable)
                holder.binding.seatTxt.setTextColor(context.getColor(R.color.black))
            }
            Seat.SeatStatus.SELECTED -> {
                holder.binding.seatTxt.setBackgroundResource(R.drawable.ic_seat_selected)
                holder.binding.seatTxt.setTextColor(context.getColor(R.color.grey))
            }
        }
        holder.binding.seatTxt.setOnClickListener {
            when(seat.status){
                Seat.SeatStatus.AVAILABLE -> {
                    seat.status = Seat.SeatStatus.SELECTED
                    selectedSeatName.add(seat.name)
                    notifyItemChanged(position)
                }
                Seat.SeatStatus.SELECTED -> {
                    seat.status = Seat.SeatStatus.AVAILABLE
                    selectedSeatName.remove(seat.name)
                    notifyItemChanged(position)
                }
                else -> {}
            }
            val selected = selectedSeatName.joinToString(", ")
            selectedSeat.Return(selected, selectedSeatName.size)
        }
    }

    override fun getItemCount(): Int = seatList.size

    interface SelectedSeat{
        fun Return(selectedName : String, num : Int)

    }
}