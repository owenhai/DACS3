package com.example.dacs3.activity

import android.os.Bundle
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
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SeatListActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySeatListBinding
    private lateinit var film : Film
    private var price : Double = 0.0
    private var number : Int = 0
    private var selectedSeatNames: String = ""
    private var selectedDate: String? = null
    private var selectedTime: String? = null
    private val database = com.google.firebase.database.FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySeatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentExtra()
        setVariable()

        initTimeDateList()
        // initSeatsList() sẽ được gọi bên trong initTimeDateList hoặc khi chọn date/time

        binding.button2.setOnClickListener {
            val selectedDate = this.selectedDate
            val selectedTime = this.selectedTime

            if (selectedDate == null || selectedTime == null || number == 0) {
                android.widget.Toast.makeText(this@SeatListActivity, "Please select date, time and seats", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Transition to Bill logic
            buyTickets(selectedDate, selectedTime)
        }
    }

    private fun initSeatsList() {
        if (selectedDate == null || selectedTime == null) {
            binding.seatRecyclerView.adapter = null
            return
        }

        val movieTitle = (film.Title ?: "Unknown").trim()
        val dateKey = selectedDate!!.replace("/", "_")
        val timeKey = selectedTime!!.replace(":", "_").replace(" ", "_")

        // Sử dụng addValueEventListener để cập nhật trạng thái ghế theo thời gian thực
        database.getReference("OccupiedSeats")
            .child(movieTitle)
            .child(dateKey)
            .child(timeKey)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val occupiedSeats = mutableSetOf<String>()
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            occupiedSeats.add(child.key ?: "")
                        }
                    }
                    renderSeats(occupiedSeats)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("SeatListActivity", "Firebase error: ${error.message}")
                }
            })
    }

    private fun renderSeats(occupiedSeats: Set<String>) {
        val gridLayoutManager = GridLayoutManager(this, 7)

        binding.apply {
            seatRecyclerView.layoutManager = gridLayoutManager

            val seatList = mutableListOf<Seat>()
            val numberSeats = 81
            for (i in 0 until numberSeats) {
                val row = (i / 7) + 1
                val col = (i % 7) + 1
                val seatName = "${('A' + (row - 1))}$col"  // A1, A2, ..., B1, B2, ...
                val seatStatus = if (occupiedSeats.contains(seatName)) {
                    Seat.SeatStatus.UNAVAILABLE
                } else {
                    Seat.SeatStatus.AVAILABLE
                }
                seatList.add(Seat(seatStatus, seatName))
            }

            // Fallback price logic
            val unitPrice = if (film.Price > 0) film.Price else 5.0

            // Reset selection when date/time changes
            numberSelectedTxt.text = "0 Seats Selected"
            priceTxt.text = "$0.00"
            price = 0.0
            number = 0
            selectedSeatNames = ""

            val seatAdapter = SeatListAdapter(seatList, this@SeatListActivity , object : SeatListAdapter.SelectedSeat {
                override fun Return(selectedName : String, num : Int) {
                    numberSelectedTxt.text = "$num Seats Selected"
                    price = (num * unitPrice)
                    number = num
                    selectedSeatNames = selectedName
                    val df = DecimalFormat("#,##0.00")
                    priceTxt.text = "$${df.format(price)}"
                }
            })
            seatRecyclerView.adapter = seatAdapter
            seatRecyclerView.isNestedScrollingEnabled = false
        }
    }

    private fun initTimeDateList() {
        binding.apply {
            dateRecyclerview.layoutManager =
                LinearLayoutManager(
                    this@SeatListActivity ,
                    LinearLayoutManager.HORIZONTAL,
                    false)

            val movieTitle = (film.Title ?: "Unknown").trim()
            database.getReference("Schedules").child(movieTitle).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Use custom schedules
                    val customDates = mutableListOf<String>()
                    val calendarMap = mutableMapOf<String, List<String>>()

                    for (child in snapshot.children) {
                        val d = child.child("date").value?.toString() ?: ""
                        val t = child.child("timeSlots").value as? List<String> ?: listOf()
                        if (d.isNotEmpty()) {
                            customDates.add(d)
                            calendarMap[d] = t
                        }
                    }

                    if (customDates.isNotEmpty()) {
                        selectedDate = customDates[0]
                        val dateAdapter = DateAdapter(customDates) { date ->
                            selectedDate = date
                            // Update times based on selected date
                            val newTimes = calendarMap[date] ?: listOf()
                            updateTimes(newTimes)
                        }
                        dateRecyclerview.adapter = dateAdapter
                        updateTimes(calendarMap[customDates[0]] ?: listOf())
                        return@addOnSuccessListener
                    }
                }

                // Fallback to default generation if no custom schedule
                setupDefaultSchedules()
            }
        }
    }

    private fun updateTimes(times: List<String>) {
        binding.TimeRecyclerview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        selectedTime = if (times.isNotEmpty()) times[0] else null
        val timeAdapter = TimeAdapter(times) { time ->
            selectedTime = time
            initSeatsList()
        }
        binding.TimeRecyclerview.adapter = timeAdapter
        initSeatsList()
    }

    private fun setupDefaultSchedules() {
        val dates = generateDates()
        selectedDate = dates[0]
        val dateAdapter = DateAdapter(dates) { date ->
            selectedDate = date
            initSeatsList()
        }
        binding.dateRecyclerview.adapter = dateAdapter

        binding.TimeRecyclerview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val times = generateTimeSlots()
        selectedTime = times[0]
        val timeAdapter = TimeAdapter(times) { time ->
            selectedTime = time
            initSeatsList()
        }
        binding.TimeRecyclerview.adapter = timeAdapter

        initSeatsList()
    }

    private fun buyTickets(date: String, time: String) {
        val sharedPref = getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val customUserId = sharedPref.getString("customUserId", "UnknownUser") ?: "UnknownUser"
        
        val movieTitle = (film.Title ?: "Unknown").trim()
        val dateKey = date.replace("/", "_")
        val timeKey = time.replace(":", "_").replace(" ", "_")

        val occupiedSeatsRef = database.getReference("OccupiedSeats")
            .child(movieTitle)
            .child(dateKey)
            .child(timeKey)

        // Sử dụng Transaction để đảm bảo không có hai người đặt cùng một ghế tại cùng một thời điểm
        occupiedSeatsRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val wantedSeats = selectedSeatNames.split(", ")

                // Kiểm tra từng ghế xem có ai đặt chưa
                for (seat in wantedSeats) {
                    if (currentData.hasChild(seat)) {
                        return com.google.firebase.database.Transaction.abort()
                    }
                }

                // Nếu tất cả ghế đều trống, đánh dấu là đã đặt
                for (seat in wantedSeats) {
                    currentData.child(seat).value = true
                }

                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {
                if (committed) {
                    // Nếu giữ ghế thành công, tiến hành tạo vé
                    val ticketId = "TKT${System.currentTimeMillis()}"
                    val ticket = Ticket(
                        ticketId = ticketId,
                        movieTitle = movieTitle,
                        showDate = date,
                        showTime = time,
                        seatNo = selectedSeatNames,
                        totalPrice = price,
                        status = "Pending",
                        userId = customUserId,
                        createdAt = System.currentTimeMillis()
                    )

                    database.getReference("Tickets").child(ticketId).setValue(ticket)
                        .addOnSuccessListener {
                            val intent = android.content.Intent(this@SeatListActivity, BillActivity::class.java)
                            intent.putExtra("ticket", ticket)
                            startActivity(intent)
                        }
                } else {
                    // Nếu commit thất bại (do có người đặt nhanh hơn)
                    android.widget.Toast.makeText(this@SeatListActivity, "Sorry, some seats were just taken!", android.widget.Toast.LENGTH_SHORT).show()
                    initSeatsList() // Cập nhật lại màn hình ghế
                }
            }
        })
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }
        binding.priceTxt.text = "$0.00"
        binding.numberSelectedTxt.text = "0 Seats Selected"
    }

    private fun getIntentExtra() {
        film = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("film", Film::class.java)!!
        } else {
            intent.getSerializableExtra("film") as Film
        }
    }

    private fun generateDates(): List<String> {
        val dates = mutableListOf<String>()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("EEE/dd/MMM")

        for (i in 0 until 7){
            dates.add(today.plusDays(i.toLong()).format(formatter))
        }
        return dates
    }

    private fun generateTimeSlots(): List<String> {
        val timeSlots = mutableListOf<String>()
        val formatter = DateTimeFormatter.ofPattern("HH:mm a")

        for (i in 0 until 24 step 2) {
            val time = LocalDate.now().atTime(i, 0).format(formatter)
            timeSlots.add(time)
        }
        return timeSlots
    }

}