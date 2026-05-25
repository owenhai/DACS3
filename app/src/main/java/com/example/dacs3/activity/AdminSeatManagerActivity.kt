package com.example.dacs3.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.adapter.DateAdapter
import com.example.dacs3.adapter.SeatListAdapter
import com.example.dacs3.adapter.TimeAdapter
import com.example.dacs3.databinding.ActivityAdminSeatManagerBinding
import com.example.dacs3.model.Seat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AdminSeatManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminSeatManagerBinding
    private val database = FirebaseDatabase.getInstance()
    private var movieTitle: String? = null
    private var selectedDate: String? = null
    private var selectedTime: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminSeatManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        movieTitle = intent.getStringExtra("movieTitle")
        binding.titleTxt.text = "Seats: $movieTitle"

        binding.backBtn.setOnClickListener { finish() }

        initScheduleLists()
    }

    private fun initScheduleLists() {
        binding.dateRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.timeRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        movieTitle?.let { title ->
            database.getReference("Schedules").child(title).get().addOnSuccessListener { snapshot ->
                val dates = mutableListOf<String>()
                val calendarMap = mutableMapOf<String, List<String>>()

                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val d = child.child("date").value?.toString() ?: ""
                        val t = child.child("timeSlots").value as? List<String> ?: listOf()
                        if (d.isNotEmpty()) {
                            dates.add(d)
                            calendarMap[d] = t
                        }
                    }
                }

                if (dates.isEmpty()) {
                    // Fallback to defaults
                    val defaultDates = generateDefaultDates()
                    val defaultTimes = generateDefaultTimes()
                    setupAdapters(defaultDates, mapOf(defaultDates[0] to defaultTimes))
                } else {
                    setupAdapters(dates, calendarMap)
                }
            }
        }
    }

    private fun setupAdapters(dates: List<String>, calendarMap: Map<String, List<String>>) {
        selectedDate = dates[0]
        val dateAdapter = DateAdapter(dates) { date ->
            selectedDate = date
            val times = calendarMap[date] ?: calendarMap.values.firstOrNull() ?: listOf()
            updateTimes(times)
        }
        binding.dateRecyclerView.adapter = dateAdapter

        val initialTimes = calendarMap[dates[0]] ?: calendarMap.values.firstOrNull() ?: listOf()
        updateTimes(initialTimes)
    }

    private fun updateTimes(times: List<String>) {
        selectedTime = if (times.isNotEmpty()) times[0] else null
        val timeAdapter = TimeAdapter(times) { time ->
            selectedTime = time
            loadSeats()
        }
        binding.timeRecyclerView.adapter = timeAdapter
        loadSeats()
    }

    private fun loadSeats() {
        if (selectedDate == null || selectedTime == null) return

        val dateKey = selectedDate!!.replace("/", "_")
        val timeKey = selectedTime!!.replace(":", "_").replace(" ", "_")

        database.getReference("OccupiedSeats")
            .child(movieTitle!!)
            .child(dateKey)
            .child(timeKey)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val occupiedSeats = mutableSetOf<String>()
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            occupiedSeats.add(child.key ?: "")
                        }
                    }
                    displaySeats(occupiedSeats)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun displaySeats(occupiedSeats: Set<String>) {
        binding.seatRecyclerView.layoutManager = GridLayoutManager(this, 7)
        val seatList = mutableListOf<Seat>()
        for (i in 0 until 81) {
            val row = (i / 7) + 1
            val col = (i % 7) + 1
            val name = "${('A' + (row - 1))}$col"
            val status = if (occupiedSeats.contains(name)) Seat.SeatStatus.UNAVAILABLE else Seat.SeatStatus.AVAILABLE
            seatList.add(Seat(status, name))
        }

        val adapter = SeatListAdapter(seatList, this, object : SeatListAdapter.SelectedSeat {
            override fun Return(selectedName: String, num: Int) {
                // In Admin mode, clicking a seat toggles its status
                if (selectedName.isNotEmpty()) {
                    toggleSeatStatus(selectedName)
                }
            }
        })
        binding.seatRecyclerView.adapter = adapter
    }

    private fun toggleSeatStatus(seatName: String) {
        val dateKey = selectedDate!!.replace("/", "_")
        val timeKey = selectedTime!!.replace(":", "_").replace(" ", "_")
        val ref = database.getReference("OccupiedSeats")
            .child(movieTitle!!)
            .child(dateKey)
            .child(timeKey)
            .child(seatName)

        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                ref.removeValue() // Make available
                Toast.makeText(this, "$seatName made AVAILABLE", Toast.LENGTH_SHORT).show()
            } else {
                ref.setValue(true) // Make occupied
                Toast.makeText(this, "$seatName made OCCUPIED", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateDefaultDates(): List<String> {
        val dates = mutableListOf<String>()
        val formatter = DateTimeFormatter.ofPattern("EEE/dd/MMM")
        for (i in 0 until 7) dates.add(LocalDate.now().plusDays(i.toLong()).format(formatter))
        return dates
    }

    private fun generateDefaultTimes(): List<String> {
        val times = mutableListOf<String>()
        val formatter = DateTimeFormatter.ofPattern("HH:mm a")
        for (i in 0 until 24 step 2) times.add(LocalDate.now().atTime(i, 0).format(formatter))
        return times
    }
}

