package com.example.dacs3.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.R
import com.example.dacs3.databinding.ViewholderTicketBinding
import com.example.dacs3.model.Ticket

class TicketListAdapter(
    private val tickets: List<Ticket>,
    private val onCancelClick: ((Ticket) -> Unit)? = null,
    private val onItemClick: (Ticket) -> Unit
) : RecyclerView.Adapter<TicketListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ViewholderTicketBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ticket = tickets[position]
        val df = java.text.DecimalFormat("#,###")

        holder.binding.movieTitleTxt.text = ticket.movieTitle
        holder.binding.dateTxt.text = "${ticket.showDate} | ${ticket.showTime}"
        holder.binding.seatsTxt.text = "Seats: ${ticket.seatNo}"
        holder.binding.priceTxt.text = "$${df.format(ticket.totalPrice)}"

        holder.binding.statusTxt.text = ticket.status
        when (ticket.status) {
            "Pending" -> {
                holder.binding.statusTxt.setTextColor(Color.YELLOW)
                holder.binding.cancelBtn.visibility = android.view.View.VISIBLE
            }
            "Paid" -> {
                holder.binding.statusTxt.setTextColor(Color.GREEN)
                holder.binding.cancelBtn.visibility = android.view.View.GONE
            }
            "Used" -> {
                holder.binding.statusTxt.setTextColor(Color.GRAY)
                holder.binding.cancelBtn.visibility = android.view.View.GONE
            }
        }

        holder.binding.cancelBtn.setOnClickListener {
            onCancelClick?.invoke(ticket)
        }

        holder.itemView.setOnClickListener { onItemClick(ticket) }
    }

    override fun getItemCount(): Int = tickets.size
}
