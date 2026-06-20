package com.example.dacs3.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ViewholderAdminScheduleBinding
import com.example.dacs3.model.ScheduleModel

class AdminScheduleAdapter(
    private val schedules: List<ScheduleModel>,
    private val onEditClick: (ScheduleModel) -> Unit,
    private val onDeleteClick: (ScheduleModel) -> Unit
) : RecyclerView.Adapter<AdminScheduleAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewholderAdminScheduleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderAdminScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.binding.dateTxt.text = schedule.date
        holder.binding.roomTxt.text = "🏛️ ${schedule.roomName}"
        holder.binding.timeTxt.text = schedule.timeSlots.firstOrNull() ?: ""

        holder.binding.editBtn.setOnClickListener { onEditClick(schedule) }
        holder.binding.deleteBtn.setOnClickListener { onDeleteClick(schedule) }
    }

    override fun getItemCount(): Int = schedules.size
}
