package com.example.dacs3.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.adapter.TicketListAdapter
import com.example.dacs3.databinding.ActivityAdminTicketsBinding
import com.example.dacs3.model.Ticket
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminTicketsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminTicketsBinding
    private val database = FirebaseDatabase.getInstance()
    private val allTickets = mutableListOf<Ticket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminTicketsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        binding.backBtn.setOnClickListener { finish() }
        loadAllTickets()
    }

    private fun setupRecyclerView() {
        binding.adminTicketRecyclerView.layoutManager = LinearLayoutManager(this)
        // Admin gets the regular adapter but we can customize behavior
        binding.adminTicketRecyclerView.adapter = TicketListAdapter(
            allTickets,
            onCancelClick = { ticket ->
                showDeleteConfirm(ticket)
            },
            onItemClick = { ticket ->
                // Go to detail?
            }
        )
    }

    private fun loadAllTickets() {
        binding.progressBar.visibility = View.VISIBLE
        database.getReference("Tickets").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTickets.clear()
                for (child in snapshot.children) {
                    val ticket = child.getValue(Ticket::class.java)
                    ticket?.let { allTickets.add(it) }
                }
                allTickets.reverse() // Newest first
                binding.adminTicketRecyclerView.adapter?.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun showDeleteConfirm(ticket: Ticket) {
        AlertDialog.Builder(this)
            .setTitle("Manage Ticket")
            .setMessage("What do you want to do with ticket ${ticket.ticketId}?")
            .setPositiveButton("DELETE") { _, _ ->
                database.getReference("Tickets").child(ticket.ticketId).removeValue()
                    .addOnSuccessListener {
                        // Also free seats if it was paid/pending
                        freeSeats(ticket)
                        Toast.makeText(this, "Ticket deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNeutralButton("MARK USED") { _, _ ->
                 database.getReference("Tickets").child(ticket.ticketId).child("status").setValue("Used")
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun freeSeats(ticket: Ticket) {
        val dateKey = ticket.showDate.replace("/", "_")
        val timeKey = ticket.showTime.replace(":", "_").replace(" ", "_")
        val seats = ticket.seatNo.split(", ")

        val occupiedSeatsRef = database.getReference("OccupiedSeats")
            .child(ticket.movieTitle)
            .child(dateKey)
            .child(timeKey)

        for (seat in seats) {
            occupiedSeatsRef.child(seat).removeValue()
        }
    }
}
