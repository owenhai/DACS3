package com.example.dacs3.activity

import android.os.Bundle
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.adapter.AdminScheduleAdapter
import com.example.dacs3.model.ScheduleModel
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import com.example.dacs3.model.CinemaRoom
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAdminMovieSchedulesBinding
import com.example.dacs3.R
import com.google.firebase.database.FirebaseDatabase
import com.example.dacs3.model.Film

class AdminMovieSchedulesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminMovieSchedulesBinding
    private val database = FirebaseDatabase.getInstance()
    private var film: Film? = null
    private var movieTitle: String? = null
    private val scheduleList = mutableListOf<ScheduleModel>()
    private lateinit var scheduleAdapter: AdminScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminMovieSchedulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        film = IntentCompat.getSerializableExtra(intent, "film", Film::class.java)
        movieTitle = film?.Title ?: intent.getStringExtra("movieTitle")
        binding.titleTxt.text = "Schedules: $movieTitle"

        binding.backBtn.setOnClickListener { finish() }

        setupRecyclerView()

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

    private fun setupRecyclerView() {
        scheduleAdapter = AdminScheduleAdapter(scheduleList,
            onEditClick = { schedule ->
                showEditScheduleDialog(schedule)
            },
            onDeleteClick = { schedule ->
                showDeleteConfirm(schedule)
            }
        )
        binding.schedulesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.schedulesRecyclerView.adapter = scheduleAdapter
    }

    private fun loadCurrentSchedules() {
        movieTitle?.let { title ->
            database.getReference("SchedulesByMovie").child(title)
                .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        scheduleList.clear()
                        for (sessionSnapshot in snapshot.children) {
                            val schedule = sessionSnapshot.getValue(ScheduleModel::class.java)
                            // Filter out "ghost" items that don't have essential data
                            if (schedule != null && schedule.date.isNotEmpty() && schedule.roomId.isNotEmpty()) {
                                schedule.sessionId = sessionSnapshot.key ?: ""
                                scheduleList.add(schedule)
                            }
                        }
                        // Sort by date then start time
                        scheduleList.sortBy { it.date + it.timeSlots.firstOrNull() }
                        scheduleAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })
        }
    }

    private fun showDeleteConfirm(schedule: ScheduleModel) {
        AlertDialog.Builder(this)
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete this session at ${schedule.timeSlots.firstOrNull()}?")
            .setPositiveButton("DELETE") { _, _ ->
                val dateKey = schedule.date.replace("/", "_")
                
                // 1. Remove from the main Schedules tree (used for seat booking)
                database.getReference("Schedules")
                    .child(dateKey)
                    .child(schedule.roomId)
                    .child(schedule.sessionId)
                    .removeValue()

                // 2. Remove from the movie-specific lookup tree
                database.getReference("SchedulesByMovie")
                    .child(schedule.movieTitle)
                    .child(schedule.sessionId)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Schedule deleted successfully", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditScheduleDialog(schedule: ScheduleModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_schedule, null)
        val durationInfoTxt = dialogView.findViewById<TextView>(R.id.durationInfoTxt)
        val dateEdt = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dateEdt)
        val timeChipGroup = dialogView.findViewById<ChipGroup>(R.id.timeChipGroup)
        val addTimeChip = dialogView.findViewById<Chip>(R.id.addTimeChip)
        val roomAutoComplete = dialogView.findViewById<AutoCompleteTextView>(R.id.roomAutoComplete)

        val movieDuration = parseDuration(film?.Time)
        durationInfoTxt.text = "Editing Schedule (+15m buffer)"
        
        // Pre-fill data
        dateEdt.setText(schedule.date)
        roomAutoComplete.setText(schedule.roomName, false)
        selectedRoom = roomsList.find { it.id == schedule.roomId }
        
        selectedTimes.clear()
        val currentTime = schedule.timeSlots.firstOrNull() ?: ""
        if (currentTime.contains(" - ")) {
            val startTime = currentTime.split(" - ")[0]
            selectedTimes.add(startTime)
            addTimeChipToGroup(timeChipGroup, startTime, addTimeChip)
        }

        // Fetch rooms for dropdown
        database.getReference("Rooms").get().addOnSuccessListener { snapshot ->
            roomsList.clear()
            val roomNames = mutableListOf<String>()
            for (child in snapshot.children) {
                val room = child.getValue(CinemaRoom::class.java)
                room?.let {
                    it.id = child.key ?: ""
                    roomsList.add(it)
                    roomNames.add(it.name)
                }
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roomNames)
            roomAutoComplete.setAdapter(adapter)
        }

        roomAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedRoom = roomsList[position]
        }

        dateEdt.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                dateEdt.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        addTimeChip.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).build()
            timePicker.addOnPositiveButtonClickListener {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                cal.set(Calendar.MINUTE, timePicker.minute)
                val time = SimpleDateFormat("hh:mm a", Locale.US).format(cal.time)
                if (!selectedTimes.contains(time)) {
                    selectedTimes.add(time)
                    addTimeChipToGroup(timeChipGroup, time, addTimeChip)
                }
            }
            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Schedule")
            .setView(dialogView)
            .setPositiveButton("UPDATE") { _, _ ->
                val date = dateEdt.text.toString()
                if (date.isNotEmpty() && selectedTimes.isNotEmpty() && selectedRoom != null) {
                    val processedTime = calculateEndTime(selectedTimes.first(), movieDuration)
                    updateSchedule(schedule, date, processedTime, selectedRoom!!)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSchedule(oldSchedule: ScheduleModel, newDate: String, newTimeSlot: String, newRoom: CinemaRoom) {
        val title = movieTitle ?: return
        
        // 1. Delete old record
        val oldDateKey = oldSchedule.date.replace("/", "_")
        database.getReference("Schedules").child(oldDateKey).child(oldSchedule.roomId).child(oldSchedule.sessionId).removeValue()
        
        // 2. Save as updated record (keeping same Session ID)
        val newDateKey = newDate.replace("/", "_")
        val scheduleData = ScheduleModel(
            sessionId = oldSchedule.sessionId,
            movieTitle = title,
            movieId = oldSchedule.movieId,
            roomName = newRoom.name,
            roomId = newRoom.id,
            date = newDate,
            timeSlots = listOf(newTimeSlot),
            timestamp = System.currentTimeMillis()
        )

        database.getReference("Schedules").child(newDateKey).child(newRoom.id).child(oldSchedule.sessionId).setValue(scheduleData)
        database.getReference("SchedulesByMovie").child(title).child(oldSchedule.sessionId).setValue(scheduleData)
            .addOnSuccessListener {
                Toast.makeText(this, "Schedule updated!", Toast.LENGTH_SHORT).show()
            }
    }

    private val selectedTimes = mutableListOf<String>()
    private val roomsList = mutableListOf<CinemaRoom>()
    private var selectedRoom: CinemaRoom? = null

    private fun showAddScheduleDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_schedule, null)
        val durationInfoTxt = dialogView.findViewById<TextView>(R.id.durationInfoTxt)
        val dateEdt = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dateEdt)
        val timeChipGroup = dialogView.findViewById<ChipGroup>(R.id.timeChipGroup)
        val addTimeChip = dialogView.findViewById<Chip>(R.id.addTimeChip)
        val roomAutoComplete = dialogView.findViewById<AutoCompleteTextView>(R.id.roomAutoComplete)

        val movieDuration = parseDuration(film?.Time)
        durationInfoTxt.text = "Movie Duration: $movieDuration mins (+15m buffer)"
        selectedTimes.clear()

        // Fetch rooms for dropdown
        database.getReference("Rooms").get().addOnSuccessListener { snapshot ->
            roomsList.clear()
            val roomNames = mutableListOf<String>()
            for (child in snapshot.children) {
                val room = child.getValue(CinemaRoom::class.java)
                room?.let {
                    it.id = child.key ?: ""
                    roomsList.add(it)
                    roomNames.add(it.name)
                }
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roomNames)
            roomAutoComplete.setAdapter(adapter)
        }

        roomAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedRoom = roomsList[position]
        }

        // Date Picker
        dateEdt.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateEdt.setText(format.format(calendar.time))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Time Picker
        addTimeChip.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Start Time")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)
                
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
                val startTimeFormatted = timeFormat.format(calendar.time)

                if (!selectedTimes.contains(startTimeFormatted)) {
                    selectedTimes.add(startTimeFormatted)
                    addTimeChipToGroup(timeChipGroup, startTimeFormatted, addTimeChip)
                }
            }
            timePicker.show(supportFragmentManager, "TIME_PICKER")
        }

        AlertDialog.Builder(this)
            .setTitle("Add Schedule for $movieTitle")
            .setView(dialogView)
            .setPositiveButton("SAVE") { _, _ ->
                val date = dateEdt.text.toString()
                if (date.isNotEmpty() && selectedTimes.isNotEmpty() && selectedRoom != null) {
                    val processedTimes = selectedTimes.sorted().map { startTime ->
                        calculateEndTime(startTime, movieDuration)
                    }
                    saveSchedule(date, processedTimes, selectedRoom!!)
                } else {
                    Toast.makeText(this, "Please select room, date and at least one time", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("CLEAR ALL") { _, _ -> deleteSchedules() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTimeChipToGroup(chipGroup: ChipGroup, time: String, addChip: Chip) {
        val chip = Chip(this)
        chip.text = time
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            chipGroup.removeView(chip)
            selectedTimes.remove(time)
        }
        // Insert before the "Add" chip
        val index = chipGroup.indexOfChild(addChip)
        chipGroup.addView(chip, index)
    }

    private fun deleteSchedules() {
        movieTitle?.let { title ->
            AlertDialog.Builder(this)
                .setTitle("Clear All Schedules")
                .setMessage("This will remove all sessions for '$title' from the lookup. Existing bookings in 'Schedules' tree may remain.")
                .setPositiveButton("CLEAR") { _, _ ->
                    database.getReference("SchedulesByMovie").child(title).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "All lookup references cleared", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun parseDuration(timeStr: String?): Int {
        if (timeStr == null) return 120 // Default 2 hours
        val digits = timeStr.filter { it.isDigit() }
        return if (digits.isNotEmpty()) digits.toInt() else 120
    }

    private fun calculateEndTime(startTimeStr: String, durationMins: Int): String {
        try {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
            val date = timeFormat.parse(startTimeStr)
            val calendar = Calendar.getInstance()
            calendar.time = date!!
            calendar.add(Calendar.MINUTE, durationMins + 15) // +15 mins buffer

            val endTimeStr = timeFormat.format(calendar.time)
            return "$startTimeStr - $endTimeStr"
        } catch (e: Exception) {
            return startTimeStr
        }
    }

    private fun checkOverlap(newTimes: List<String>, existingSchedules: Map<String, List<String>>, currentMovieTitle: String): String? {
        val format = SimpleDateFormat("hh:mm a", Locale.US)
        
        fun parseInterval(slot: String): Pair<Long, Long>? {
            val parts = slot.split(" - ")
            if (parts.size == 2) {
                val start = format.parse(parts[0])?.time ?: 0L
                val end = format.parse(parts[1])?.time ?: 0L
                return start to end
            }
            return null
        }

        val newIntervals = newTimes.mapNotNull { parseInterval(it) }

        val sortedNew = newIntervals.sortedBy { it.first }
        for (i in 0 until sortedNew.size - 1) {
            if (sortedNew[i].second > sortedNew[i + 1].first) {
                return "Conflict within selected times: ${format.format(sortedNew[i].first)} overlaps with ${format.format(sortedNew[i+1].first)}"
            }
        }

        for ((movieTitle, timeSlots) in existingSchedules) {
            if (movieTitle == currentMovieTitle) continue
            
            for (existingSlot in timeSlots) {
                val existingInterval = parseInterval(existingSlot) ?: continue
                for (newInterval in newIntervals) {
                    if (newInterval.first < existingInterval.second && existingInterval.first < newInterval.second) {
                        return "Conflict: Room already occupied by '$movieTitle' from $existingSlot"
                    }
                }
            }
        }
        return null
    }

    private fun saveSchedule(date: String, times: List<String>, room: CinemaRoom) {
        val title = movieTitle ?: return
        val dateKey = date.replace("/", "_")

        database.getReference("Schedules").child(dateKey).child(room.id).get()
            .addOnSuccessListener { snapshot ->
                val existingSchedules = mutableMapOf<String, List<String>>()
                for (sessionSnapshot in snapshot.children) {
                    val mTitle = sessionSnapshot.child("movieTitle").value?.toString() ?: continue
                    val slots = (sessionSnapshot.child("timeSlots").value as? List<*>)?.filterIsInstance<String>() ?: listOf()
                    
                    if (!existingSchedules.containsKey(mTitle)) {
                        existingSchedules[mTitle] = mutableListOf()
                    }
                    (existingSchedules[mTitle] as MutableList).addAll(slots)
                }

                val overlapError = checkOverlap(times, existingSchedules, title)
                if (overlapError != null) {
                    AlertDialog.Builder(this)
                        .setTitle("Schedule Conflict")
                        .setMessage(overlapError)
                        .setPositiveButton("OK", null)
                        .show()
                    return@addOnSuccessListener
                }

                // Save each time as a separate session
                for (time in times) {
                    val sessionRef = database.getReference("Schedules").child(dateKey).child(room.id).push()
                    val sessionId = sessionRef.key ?: ""

                    val scheduleData = ScheduleModel(
                        sessionId = sessionId,
                        movieTitle = title,
                        movieId = (film?.key ?: ""),
                        roomName = room.name,
                        roomId = room.id,
                        date = date,
                        timeSlots = listOf(time),
                        timestamp = System.currentTimeMillis()
                    )

                    sessionRef.setValue(scheduleData)
                    database.getReference("SchedulesByMovie").child(title).child(sessionId).setValue(scheduleData)
                }
                Toast.makeText(this, "Schedules saved successfully!", Toast.LENGTH_SHORT).show()
                loadCurrentSchedules()
            }
    }
}
