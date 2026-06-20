package com.example.dacs3.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.R
import com.example.dacs3.adapter.DateAdapter
import com.example.dacs3.adapter.SeatListAdapter
import com.example.dacs3.adapter.TimeAdapter
import com.example.dacs3.databinding.ActivitySeatListBinding
import com.example.dacs3.model.Film
import com.example.dacs3.model.Seat
import com.example.dacs3.model.Ticket
import com.google.firebase.database.FirebaseDatabase

class SeatListActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySeatListBinding
    private lateinit var film: Film
    private var price: Double = 0.0
    private var number: Int = 0
    private var selectedSeatNames: String = ""
    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private var selectedSessionId: String? = null
    private var currentRoomRows: Int = 9
    private var currentRoomCols: Int = 9
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySeatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentExtra()
        binding.backBtn.setOnClickListener { finish() }
        
        initScheduleLists()

        binding.button2.setOnClickListener {
            if (selectedDate == null || selectedTime == null || number == 0 || selectedSessionId == null) {
                Toast.makeText(this, "Please select date, time and seats", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buyTickets(selectedDate!!, selectedTime!!)
        }
    }

    private fun initScheduleLists() {
        val movieTitle = (film.Title ?: "Unknown").trim()
        database.getReference("SchedulesByMovie").child(movieTitle).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val dates = mutableListOf<String>()
                val sessionMap = mutableMapOf<String, MutableList<com.example.dacs3.model.ScheduleModel>>()

                for (child in snapshot.children) {
                    val schedule = child.getValue(com.example.dacs3.model.ScheduleModel::class.java)
                    if (schedule != null && !schedule.date.isNullOrEmpty() && schedule.date.contains("/")) {
                        schedule.sessionId = child.key ?: ""
                        if (!dates.contains(schedule.date)) dates.add(schedule.date)
                        if (!sessionMap.containsKey(schedule.date)) sessionMap[schedule.date] = mutableListOf()
                        sessionMap[schedule.date]?.add(schedule)
                    }
                }

                if (dates.isNotEmpty()) {
                    dates.sort()
                    selectedDate = dates[0]
                    binding.dateRecyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.dateRecyclerview.adapter = DateAdapter(dates) { date ->
                        selectedDate = date
                        updateTimes(sessionMap[date] ?: listOf())
                    }
                    updateTimes(sessionMap[dates[0]] ?: listOf())
                    return@addOnSuccessListener
                }
            }
            Toast.makeText(this, "No valid schedule found", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateTimes(sessions: List<com.example.dacs3.model.ScheduleModel>) {
        val timeStrings = sessions.map { it.timeSlots.firstOrNull() ?: "" }
        selectedTime = if (timeStrings.isNotEmpty()) timeStrings[0] else null
        resetSelection()

        binding.TimeRecyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.TimeRecyclerview.adapter = TimeAdapter(timeStrings) { time ->
            val session = sessions.find { it.timeSlots.firstOrNull() == time }
            session?.let {
                selectedTime = time
                selectedSessionId = it.sessionId
                resetSelection()
                fetchRoomAndLoadSeats(it.roomId)
            }
        }

        if (sessions.isNotEmpty()) {
            selectedSessionId = sessions[0].sessionId
            fetchRoomAndLoadSeats(sessions[0].roomId)
        }
    }

    private fun fetchRoomAndLoadSeats(roomId: String) {
        database.getReference("Rooms").child(roomId).get().addOnSuccessListener { snapshot ->
            val room = snapshot.getValue(com.example.dacs3.model.CinemaRoom::class.java)
            currentRoomRows = room?.totalRows ?: 9
            currentRoomCols = room?.totalCols ?: 9
            initSeatsList()
        }
    }

    private fun initSeatsList() {
        val sessionId = selectedSessionId ?: return
        database.getReference("OccupiedSeats").child(sessionId)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val occupiedSeats = mutableSetOf<String>()
                    for (child in snapshot.children) occupiedSeats.add(child.key ?: "")
                    renderSeats(occupiedSeats)
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    private fun renderSeats(occupiedSeats: Set<String>) {
        val rows = if (currentRoomRows >= 3) currentRoomRows else 9
        val cols = if (currentRoomCols >= 3) currentRoomCols else 9
        
        binding.seatRecyclerView.layoutManager = GridLayoutManager(this, cols)
        
        val seatList = mutableListOf<Seat>()
        val currentSelectedList = selectedSeatNames.split(", ").filter { it.isNotEmpty() }
        val selectedSeatsSet = currentSelectedList.toSet()

        for (i in 0 until (rows * cols)) {
            val row = (i / cols) + 1
            val col = (i % cols) + 1
            val seatName = "${('A' + (row - 1))}$col"
            val status = when {
                occupiedSeats.contains(seatName) -> Seat.SeatStatus.UNAVAILABLE
                selectedSeatsSet.contains(seatName) -> Seat.SeatStatus.SELECTED
                else -> Seat.SeatStatus.AVAILABLE
            }
            seatList.add(Seat(status, seatName))
        }

        val unitPrice = if (film.Price > 0) film.Price else 45000.0
        val seatAdapter = SeatListAdapter(seatList, this, object : SeatListAdapter.SelectedSeat {
            override fun Return(selectedName: String, num: Int, clickedName: String) {
                number = num
                selectedSeatNames = selectedName
                price = num * unitPrice
                binding.numberSelectedTxt.text = "$num Seats Selected"
                binding.priceTxt.text = formatVnd(price)
            }
        }, currentSelectedList)
        binding.seatRecyclerView.adapter = seatAdapter
    }

    private fun formatVnd(amount: Double): String {
        val formatter = java.text.DecimalFormat("#,###")
        val symbols = java.text.DecimalFormatSymbols(java.util.Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        formatter.decimalFormatSymbols = symbols
        return "${formatter.format(amount)} đ"
    }

    private fun buyTickets(date: String, time: String) {
        val sharedPref = getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val customUserId = sharedPref.getString("customUserId", "UnknownUser") ?: "UnknownUser"
        val sessionId = selectedSessionId ?: return

        database.getReference("OccupiedSeats").child(sessionId).runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val wantedSeats = selectedSeatNames.split(", ")
                for (seat in wantedSeats) if (currentData.hasChild(seat)) return com.google.firebase.database.Transaction.abort()
                for (seat in wantedSeats) currentData.child(seat).value = true
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {
                if (committed) {
                    val ticketId = "TKT${System.currentTimeMillis()}"
                    val ticket = Ticket(
                        ticketId = ticketId,
                        movieTitle = film.Title ?: "",
                        showDate = date,
                        showTime = time,
                        seatNo = selectedSeatNames,
                        totalPrice = price,
                        isCheckedIn = false,
                        status = "Pending",
                        userId = customUserId,
                        createdAt = System.currentTimeMillis()
                    )
                    database.getReference("Tickets").child(ticketId).setValue(ticket).addOnSuccessListener {
                        val intent = android.content.Intent(this@SeatListActivity, FoodSelectionActivity::class.java)
                        intent.putExtra("ticket", ticket)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this@SeatListActivity, "Seats already taken!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun resetSelection() {
        selectedSeatNames = ""
        number = 0
        price = 0.0
        binding.numberSelectedTxt.text = "0 Seats Selected"
        binding.priceTxt.text = "0 đ"
    }

    private fun getIntentExtra() {
        film = intent.getSerializableExtra("film") as Film
    }
}
