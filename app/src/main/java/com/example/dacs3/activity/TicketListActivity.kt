package com.example.dacs3.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.adapter.TicketListAdapter
import com.example.dacs3.databinding.ActivityTicketListBinding
import com.example.dacs3.model.Ticket
import com.google.firebase.database.*

class TicketListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTicketListBinding
    private val database = FirebaseDatabase.getInstance()
    private val ticketList = mutableListOf<Ticket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTicketListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener { finish() }

        setupRecyclerView()
        loadUserTickets()
    }

    private fun setupRecyclerView() {
        binding.ticketRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.ticketRecyclerView.adapter = TicketListAdapter(
            ticketList,
            onCancelClick = { ticket ->
                cancelTicket(ticket)
            },
            onItemClick = { ticket ->
                val intent = Intent(this, BillActivity::class.java)
                intent.putExtra("ticket", ticket)
                startActivity(intent)
            }
        )
    }

    private fun cancelTicket(ticket: Ticket) {
        database.getReference("Tickets").child(ticket.ticketId).removeValue()
            .addOnSuccessListener {
                // Giải phóng ghế khi xóa vé
                freeSeats(ticket)
                Toast.makeText(this, "Ticket cancelled", Toast.LENGTH_SHORT).show()
            }
    }

    private fun freeSeats(ticket: Ticket) {
        if (ticket.sessionId.isEmpty()) return
        
        val seats = ticket.seatNo.split(", ")
        val occupiedSeatsRef = database.getReference("OccupiedSeats")
            .child(ticket.sessionId)

        for (seat in seats) {
            occupiedSeatsRef.child(seat).removeValue()
        }
    }

    private fun loadUserTickets() {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val customUserId = sharedPref.getString("customUserId", null)

        if (customUserId == null) {
            binding.emptyTxt.visibility = View.VISIBLE
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        database.getReference("Tickets")
            .orderByChild("userId")
            .equalTo(customUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ticketList.clear()
                    val now = System.currentTimeMillis()
                    val fiveMinutesMs = 5 * 60 * 1000

                    for (data in snapshot.children) {
                        val ticket = data.getValue(Ticket::class.java)
                        ticket?.let {
                            // Tự động xóa nếu quá 5 phút và vẫn là Pending
                            if (it.status == "Pending" && (now - it.createdAt) > fiveMinutesMs) {
                                database.getReference("Tickets").child(it.ticketId).removeValue()
                                freeSeats(it) // Giải phóng ghế khi tự động xóa
                            } else {
                                ticketList.add(it)
                            }
                        }
                    }

                    binding.progressBar.visibility = View.GONE
                    if (ticketList.isEmpty()) {
                        binding.emptyTxt.visibility = View.VISIBLE
                    } else {
                        binding.emptyTxt.visibility = View.GONE
                        ticketList.reverse() // Mới nhất lên đầu
                        binding.ticketRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                }
            })
    }
}
