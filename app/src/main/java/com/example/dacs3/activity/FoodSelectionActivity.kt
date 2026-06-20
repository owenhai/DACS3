package com.example.dacs3.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.adapter.FoodSelectionAdapter
import com.example.dacs3.databinding.ActivityFoodSelectionBinding
import com.example.dacs3.model.FoodItem
import com.example.dacs3.model.Ticket
import com.google.firebase.database.FirebaseDatabase
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class FoodSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoodSelectionBinding
    private lateinit var ticket: Ticket
    private val database = FirebaseDatabase.getInstance()
    private val foodList = mutableListOf<FoodItem>()
    private lateinit var foodAdapter: FoodSelectionAdapter
    private var totalSnackPrice = 0.0
    private var selectedFoodDetails = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ticket = intent.getSerializableExtra("ticket") as Ticket

        binding.backBtn.setOnClickListener { finish() }
        
        setupRecyclerView()
        loadFoods()

        binding.nextBtn.setOnClickListener {
            // Update the existing ticket on Firebase with food info
            val finalTotalPrice = ticket.totalPrice + totalSnackPrice
            val finalFoodDetails = selectedFoodDetails
            
            database.getReference("Tickets").child(ticket.ticketId).updateChildren(mapOf(
                "totalPrice" to finalTotalPrice,
                "foodDetails" to finalFoodDetails
            )).addOnSuccessListener {
                val intent = Intent(this, BillActivity::class.java)
                val finalTicket = ticket.copy(
                    totalPrice = finalTotalPrice,
                    foodDetails = finalFoodDetails
                )
                intent.putExtra("ticket", finalTicket)
                startActivity(intent)
            }
        }
    }

    private fun setupRecyclerView() {
        foodAdapter = FoodSelectionAdapter(foodList) { quantities ->
            calculateTotal(quantities)
        }
        binding.foodRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.foodRecyclerView.adapter = foodAdapter
    }

    private fun loadFoods() {
        database.getReference("FoodAndDrink").get().addOnSuccessListener { snapshot ->
            foodList.clear()
            for (child in snapshot.children) {
                child.getValue(FoodItem::class.java)?.let {
                    it.id = child.key ?: ""
                    foodList.add(it)
                }
            }
            foodAdapter.notifyDataSetChanged()
        }
    }

    private fun calculateTotal(quantities: Map<String, Int>) {
        totalSnackPrice = 0.0
        val details = mutableListOf<String>()
        for ((id, qty) in quantities) {
            if (qty > 0) {
                val food = foodList.find { it.id == id }
                food?.let {
                    totalSnackPrice += it.price * qty
                    details.add("${it.name} x$qty")
                }
            }
        }
        selectedFoodDetails = details.joinToString(", ")
        binding.totalSnackPriceTxt.text = formatVnd(totalSnackPrice)
    }

    private fun formatVnd(amount: Double): String {
        val formatter = DecimalFormat("#,###")
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        formatter.decimalFormatSymbols = symbols
        return "${formatter.format(amount)} đ"
    }
}
