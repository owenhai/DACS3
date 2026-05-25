package com.example.dacs3.activity

import android.os.Bundle
import android.graphics.Color
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAdminMovieSchedulesBinding
import com.example.dacs3.R
import com.google.firebase.database.FirebaseDatabase

class AdminMovieSchedulesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminMovieSchedulesBinding
    private val database = FirebaseDatabase.getInstance()
    private var movieTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminMovieSchedulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        movieTitle = intent.getStringExtra("movieTitle")
        binding.titleTxt.text = "Schedules: $movieTitle"

        binding.backBtn.setOnClickListener { finish() }

        binding.addScheduleBtn.setOnClickListener {
            showAddScheduleDialog()
        }

        binding.manageSeatsBtn.setOnClickListener {
            val intent = android.content.Intent(this, AdminSeatManagerActivity::class.java)
            intent.putExtra("movieTitle", movieTitle)
            startActivity(intent)
        }

        loadCurrentSchedules()
    }

    private fun loadCurrentSchedules() {
        // Fetch from Schedules/{movieTitle}
        movieTitle?.let { title ->
             database.getReference("Schedules").child(title).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                 override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                     if (snapshot.exists()) {
                         val sb = StringBuilder()
                         for (child in snapshot.children) {
                             val date = child.child("date").value?.toString() ?: child.key ?: ""
                             val times = child.child("timeSlots").value?.toString() ?: ""
                             sb.append("📅 $date\n⏰ $times\n\n")
                         }
                         binding.currentSchedulesTxt.text = sb.toString()
                     } else {
                         binding.currentSchedulesTxt.text = "No custom schedules. App uses defaults (Next 7 days)."
                     }
                 }

                 override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                     Toast.makeText(this@AdminMovieSchedulesActivity, "Failed to load: ${error.message}", Toast.LENGTH_SHORT).show()
                 }
             })
        }
    }

    private fun showAddScheduleDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Schedule for $movieTitle")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(60, 40, 60, 10)

        val dateInput = EditText(this)
        dateInput.hint = "Date (e.g. Mon/25/May)"
        dateInput.setHintTextColor(Color.GRAY)
        dateInput.setTextColor(Color.BLACK)
        layout.addView(dateInput)

        val timeInput = EditText(this)
        timeInput.hint = "Time Slots (comma separated, e.g. 09:00 AM, 02:00 PM)"
        timeInput.setHintTextColor(Color.GRAY)
        timeInput.setTextColor(Color.BLACK)
        layout.addView(timeInput)

        builder.setView(layout)

        builder.setPositiveButton("SAVE") { _, _ ->
            val date = dateInput.text.toString().trim()
            val times = timeInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (date.isNotEmpty() && times.isNotEmpty()) {
                saveSchedule(date, times)
            } else {
                Toast.makeText(this, "Please enter both date and times", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveSchedule(date: String, times: List<String>) {
        movieTitle?.let { title ->
            val ref = database.getReference("Schedules").child(title).child(date.replace("/", "_"))
            ref.child("date").setValue(date)
            ref.child("timeSlots").setValue(times)
                .addOnSuccessListener {
                    Toast.makeText(this, "Schedule added!", Toast.LENGTH_SHORT).show()
                    loadCurrentSchedules()
                }
        }
    }
}
