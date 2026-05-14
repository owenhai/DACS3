package com.example.dacs3.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.R
import com.example.dacs3.adapter.DateAdapter
import com.example.dacs3.adapter.TimeAdapter
import com.example.dacs3.databinding.ActivitySeatListBinding
import com.example.dacs3.model.Film
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SeatListActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySeatListBinding
    private lateinit var film : Film
    private var price : Double = 0.0
    private var number : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySeatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentExtra()
        setVariable()
        initTimeDateList()
    }

    private fun initTimeDateList() {
        binding.apply {
            dateRecyclerview.layoutManager =
                LinearLayoutManager(
                    this@SeatListActivity ,
                    LinearLayoutManager.HORIZONTAL,
                    false)
            dateRecyclerview.adapter = DateAdapter(generateDates())

            TimeRecyclerview.layoutManager =
                LinearLayoutManager(
                    this@SeatListActivity ,
                    LinearLayoutManager.HORIZONTAL,
                    false)
            TimeRecyclerview.adapter = TimeAdapter(generateTimeSlots())
        }
    }

    private fun setVariable() {
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun getIntentExtra() {
        film = intent.getSerializableExtra("film") as Film
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