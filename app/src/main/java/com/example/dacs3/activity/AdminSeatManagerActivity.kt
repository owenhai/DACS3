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
import com.example.dacs3.model.ScheduleModel
import com.example.dacs3.model.CinemaRoom
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

    private var selectedSessionId: String? = null
    private var currentRoomRows: Int = 9
    private var currentRoomCols: Int = 9

    private fun initScheduleLists() {
        binding.dateRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.timeRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        movieTitle?.let { title ->
            database.getReference("SchedulesByMovie").child(title).get().addOnSuccessListener { snapshot ->
                val dates = mutableListOf<String>()
                val sessionMap = mutableMapOf<String, MutableList<ScheduleModel>>()

                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val schedule = child.getValue(ScheduleModel::class.java)
                        schedule?.let {
                            it.sessionId = child.key ?: ""
                            if (!dates.contains(it.date)) dates.add(it.date)
                            if (!sessionMap.containsKey(it.date)) sessionMap[it.date] = mutableListOf()
                            sessionMap[it.date]?.add(it)
                        }
                    }
                }

                if (dates.isEmpty()) {
                    binding.dateRecyclerView.adapter = null
                    binding.timeRecyclerView.adapter = null
                    Toast.makeText(this, "No schedule available", Toast.LENGTH_SHORT).show()
                } else {
                    setupAdapters(dates, sessionMap)
                }
            }
        }
    }

    private fun setupAdapters(dates: List<String>, sessionMap: Map<String, List<ScheduleModel>>) {
        selectedDate = dates[0]
        val dateAdapter = DateAdapter(dates) { date ->
            selectedDate = date
            val sessions = sessionMap[date] ?: listOf()
            updateTimes(sessions)
        }
        binding.dateRecyclerView.adapter = dateAdapter

        val initialSessions = sessionMap[dates[0]] ?: listOf()
        updateTimes(initialSessions)
    }

    private fun updateTimes(sessions: List<ScheduleModel>) {
        val timeStrings = sessions.map { it.timeSlots.firstOrNull() ?: "" }
        val timeAdapter = TimeAdapter(timeStrings) { time ->
            val session = sessions.find { it.timeSlots.firstOrNull() == time }
            session?.let {
                selectedTime = time
                selectedSessionId = it.sessionId
                fetchRoomAndLoadSeats(it.roomId)
            }
        }
        binding.timeRecyclerView.adapter = timeAdapter
        
        if (sessions.isNotEmpty()) {
            selectedTime = timeStrings[0]
            selectedSessionId = sessions[0].sessionId
            fetchRoomAndLoadSeats(sessions[0].roomId)
        }
    }

    private fun fetchRoomAndLoadSeats(roomId: String) {
        database.getReference("Rooms").child(roomId).get().addOnSuccessListener { snapshot ->
            val room = snapshot.getValue(com.example.dacs3.model.CinemaRoom::class.java)
            room?.let {
                currentRoomRows = it.totalRows
                currentRoomCols = it.totalCols
                loadSeats()
            }
        }
    }

    private fun loadSeats() {
        val sessionId = selectedSessionId ?: return

        database.getReference("OccupiedSeats")
            .child(sessionId)
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
        binding.seatRecyclerView.layoutManager = GridLayoutManager(this, currentRoomCols)
        val seatList = mutableListOf<Seat>()
        val totalSeats = currentRoomRows * currentRoomCols
        
        for (i in 0 until totalSeats) {
            val row = (i / currentRoomCols) + 1
            val col = (i % currentRoomCols) + 1
            val name = "${('A' + (row - 1))}$col"
            val status = if (occupiedSeats.contains(name)) Seat.SeatStatus.UNAVAILABLE else Seat.SeatStatus.AVAILABLE
            seatList.add(Seat(status, name))
        }

        val adapter = SeatListAdapter(seatList, this, object : SeatListAdapter.SelectedSeat {
            override fun Return(selectedName: String, num: Int, clickedName: String) {
                toggleSeatStatus(clickedName)
            }
        })
        binding.seatRecyclerView.adapter = adapter
    }

    private fun toggleSeatStatus(seatName: String) {
        val sessionId = selectedSessionId ?: return
        val ref = database.getReference("OccupiedSeats")
            .child(sessionId)
            .child(seatName)

        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                ref.removeValue()
            } else {
                ref.setValue(true)
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
