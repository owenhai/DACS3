package com.example.dacs3.activity

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.R
import com.example.dacs3.adapter.AdminRoomAdapter
import com.example.dacs3.databinding.ActivityAdminRoomsBinding
import com.example.dacs3.model.CinemaRoom
import com.google.firebase.database.FirebaseDatabase

class AdminRoomsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminRoomsBinding
    private val database = FirebaseDatabase.getInstance()
    private val roomList = mutableListOf<CinemaRoom>()
    private lateinit var roomAdapter: AdminRoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminRoomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener { finish() }
        binding.addRoomBtn.setOnClickListener { showRoomDialog(null) }

        setupRecyclerView()
        loadRooms()
    }

    private fun setupRecyclerView() {
        roomAdapter = AdminRoomAdapter(roomList,
            onEditClick = { room -> showRoomDialog(room) },
            onDeleteClick = { room -> showDeleteConfirm(room) }
        )
        binding.roomsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.roomsRecyclerView.adapter = roomAdapter
    }

    private fun loadRooms() {
        database.getReference("Rooms").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                roomList.clear()
                for (child in snapshot.children) {
                    val room = child.getValue(CinemaRoom::class.java)
                    room?.let {
                        it.id = child.key ?: ""
                        roomList.add(it)
                    }
                }
                roomAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun showRoomDialog(room: CinemaRoom?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val nameEdt = EditText(this).apply { 
            hint = "Room Name"
            setText(room?.name ?: "")
        }
        
        // Dropdown for Type
        val types = arrayOf("Standard", "IMAX", "3D")
        val typeSpinner = AutoCompleteTextView(this).apply {
            hint = "Select Type"
            setAdapter(ArrayAdapter(this@AdminRoomsActivity, android.R.layout.simple_dropdown_item_1line, types))
            // Only set default if editing, otherwise keep hint
            if (room != null) {
                setText(room.type, false)
            }
            inputType = InputType.TYPE_NULL
            setOnClickListener { showDropDown() }
            setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDropDown() }
        }

        val rowsEdt = EditText(this).apply { 
            hint = "Total Rows"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(room?.totalRows?.toString() ?: "9")
        }
        val colsEdt = EditText(this).apply { 
            hint = "Total Cols"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(room?.totalCols?.toString() ?: "9")
        }

        layout.addView(nameEdt)
        layout.addView(typeSpinner)
        layout.addView(rowsEdt)
        layout.addView(colsEdt)

        AlertDialog.Builder(this)
            .setTitle(if (room == null) "Add New Room" else "Edit Room")
            .setView(layout)
            .setPositiveButton(if (room == null) "ADD" else "UPDATE") { _, _ ->
                val name = nameEdt.text.toString().trim()
                val type = typeSpinner.text.toString()
                val rows = rowsEdt.text.toString().toIntOrNull() ?: 9
                val cols = colsEdt.text.toString().toIntOrNull() ?: 9

                if (name.isNotEmpty()) {
                    val ref = if (room == null) database.getReference("Rooms").push() 
                             else database.getReference("Rooms").child(room.id)
                    
                    val newRoom = CinemaRoom(id = ref.key ?: "", name = name, type = type, totalRows = rows, totalCols = cols)
                    ref.setValue(newRoom).addOnSuccessListener {
                        Toast.makeText(this, "Room saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirm(room: CinemaRoom) {
        AlertDialog.Builder(this)
            .setTitle("Delete Room")
            .setMessage("Are you sure you want to delete '${room.name}'? This may affect existing schedules.")
            .setPositiveButton("DELETE") { _, _ ->
                database.getReference("Rooms").child(room.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Room deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
