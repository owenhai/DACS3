package com.example.dacs3.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.adapter.TicketListAdapter
import com.example.dacs3.databinding.ActivityAdminRevenueBinding
import com.example.dacs3.model.Ticket
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AdminRevenueActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminRevenueBinding
    private val database = FirebaseDatabase.getInstance()
    private val paidTickets = mutableListOf<Ticket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminRevenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener { finish() }
        setupRecyclerView()
        loadRevenueData()
    }

    private fun setupRecyclerView() {
        binding.recentSalesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.recentSalesRecyclerView.adapter = TicketListAdapter(paidTickets, {}, {})
    }

    private fun loadRevenueData() {
        database.getReference("Tickets").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRevenue = 0.0
                var soldCount = 0
                var pendingCount = 0
                paidTickets.clear()

                for (child in snapshot.children) {
                    val ticket = child.getValue(Ticket::class.java)
                    ticket?.let {
                        if (it.status == "Paid" || it.status == "Used") {
                            totalRevenue += it.totalPrice
                            soldCount++
                            paidTickets.add(it)
                        } else if (it.status == "Pending") {
                            pendingCount++
                        }
                    }
                }

                paidTickets.reverse()
                binding.totalRevenueTxt.text = formatVnd(totalRevenue)
                binding.soldCountTxt.text = soldCount.toString()
                binding.pendingCountTxt.text = pendingCount.toString()
                binding.recentSalesRecyclerView.adapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun formatVnd(amount: Double): String {
        val formatter = DecimalFormat("#,###")
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        formatter.decimalFormatSymbols = symbols
        return "${formatter.format(amount)} đ"
    }
}
