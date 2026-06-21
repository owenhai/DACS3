package com.example.dacs3.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.R
import com.example.dacs3.databinding.ViewholderAdminRoomBinding
import com.example.dacs3.model.CinemaRoom

class AdminRoomAdapter(
    private val rooms: List<CinemaRoom>,
    private val onEditClick: (CinemaRoom) -> Unit,
    private val onDeleteClick: (CinemaRoom) -> Unit
) : RecyclerView.Adapter<AdminRoomAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewholderAdminRoomBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderAdminRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = rooms[position]
        holder.binding.roomNameTxt.text = room.name
        holder.binding.roomTypeTxt.text = room.type
        holder.binding.roomInfoTxt.text = "Layout: ${room.totalRows}x${room.totalCols} (${room.totalRows * room.totalCols} seats)"

        // Set different backgrounds based on room type
        when (room.type.uppercase()) {
            "IMAX" -> holder.binding.roomTypeTxt.setBackgroundResource(R.drawable.imax_bg)
            "3D" -> holder.binding.roomTypeTxt.setBackgroundResource(R.drawable.three_d_bg)
            else -> holder.binding.roomTypeTxt.setBackgroundResource(R.drawable.standard_bg)
        }

        holder.binding.editBtn.setOnClickListener { onEditClick(room) }
        holder.binding.deleteBtn.setOnClickListener { onDeleteClick(room) }
    }

    override fun getItemCount(): Int = rooms.size
}
