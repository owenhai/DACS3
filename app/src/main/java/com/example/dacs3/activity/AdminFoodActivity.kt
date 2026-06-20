package com.example.dacs3.activity

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.adapter.AdminFoodAdapter
import com.example.dacs3.databinding.ActivityAdminFoodBinding
import com.example.dacs3.model.FoodItem
import com.google.firebase.database.FirebaseDatabase

class AdminFoodActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminFoodBinding
    private val database = FirebaseDatabase.getInstance()
    private val foodList = mutableListOf<FoodItem>()
    private lateinit var foodAdapter: AdminFoodAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener { finish() }
        binding.addFoodBtn.setOnClickListener { showFoodDialog(null) }

        setupRecyclerView()
        loadFoods()
    }

    private fun setupRecyclerView() {
        foodAdapter = AdminFoodAdapter(foodList,
            onEditClick = { food -> showFoodDialog(food) },
            onDeleteClick = { food -> showDeleteConfirm(food) }
        )
        binding.foodRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.foodRecyclerView.adapter = foodAdapter
    }

    private fun loadFoods() {
        database.getReference("FoodAndDrink").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                foodList.clear()
                for (child in snapshot.children) {
                    val food = child.getValue(FoodItem::class.java)
                    food?.let {
                        it.id = child.key ?: ""
                        foodList.add(it)
                    }
                }
                foodAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun showFoodDialog(food: FoodItem?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val nameEdt = EditText(this).apply { 
            hint = "Item Name (e.g. Popcorn)"
            setText(food?.name ?: "")
        }
        val priceEdt = EditText(this).apply { 
            hint = "Price (VND)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(food?.price?.toInt()?.toString() ?: "")
        }
        val imgUrlEdt = EditText(this).apply { 
            hint = "Image URL (Optional)"
            setText(food?.imagePath ?: "")
        }

        layout.addView(nameEdt)
        layout.addView(priceEdt)
        layout.addView(imgUrlEdt)

        AlertDialog.Builder(this)
            .setTitle(if (food == null) "Add Food/Drink" else "Edit Item")
            .setView(layout)
            .setPositiveButton(if (food == null) "ADD" else "UPDATE") { _, _ ->
                val name = nameEdt.text.toString().trim()
                val price = priceEdt.text.toString().toDoubleOrNull() ?: 0.0
                val imgUrl = imgUrlEdt.text.toString().trim()

                if (name.isNotEmpty()) {
                    val ref = if (food == null) database.getReference("FoodAndDrink").push() 
                             else database.getReference("FoodAndDrink").child(food.id)
                    
                    val newItem = FoodItem(id = ref.key ?: "", name = name, price = price, imagePath = imgUrl)
                    ref.setValue(newItem).addOnSuccessListener {
                        Toast.makeText(this, "Item saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirm(food: FoodItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Delete '${food.name}'?")
            .setPositiveButton("DELETE") { _, _ ->
                database.getReference("FoodAndDrink").child(food.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
