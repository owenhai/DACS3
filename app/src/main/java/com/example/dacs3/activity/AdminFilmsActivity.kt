package com.example.dacs3.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.adapter.AdminFilmAdapter
import com.example.dacs3.databinding.ActivityAdminFilmsBinding
import com.example.dacs3.model.Film
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminFilmsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminFilmsBinding
    private val database = FirebaseDatabase.getInstance()
    private val allFilms = mutableListOf<Film>()
    private lateinit var adapter: AdminFilmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminFilmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadFilms()
    }

    private fun setupRecyclerView() {
        adapter = AdminFilmAdapter(mutableListOf(),
            onEditClick = { film ->
                val intent = Intent(this, AdminEditFilmActivity::class.java)
                intent.putExtra("film", film)
                startActivity(intent)
            },
            onDeleteClick = { film ->
                showDeleteConfirm(film)
            }
        )
        binding.adminFilmRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.adminFilmRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.backBtn.setOnClickListener { finish() }

        binding.addFilmFab.setOnClickListener {
            startActivity(Intent(this, AdminEditFilmActivity::class.java))
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFilms(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadFilms() {
        binding.progressBar.visibility = View.VISIBLE
        // Admin manages the "Items" node (Top Movies)
        database.getReference("Items").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allFilms.clear()
                for (child in snapshot.children) {
                    val film = child.getValue(Film::class.java)
                    film?.let {
                        it.key = child.key // Capture the actual database key
                        allFilms.add(it)
                    }
                }
                adapter.updateData(allFilms)
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@AdminFilmsActivity, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterFilms(query: String) {
        val filtered = allFilms.filter { it.Title?.contains(query, ignoreCase = true) == true }
        adapter.updateData(filtered)
    }

    private fun showDeleteConfirm(film: Film) {
        AlertDialog.Builder(this)
            .setTitle("Delete Movie")
            .setMessage("Are you sure you want to delete '${film.Title}'? This will remove it from Top Movies and Upcoming.")
            .setPositiveButton("Delete") { _, _ ->
                val filmKey = film.key ?: film.Title
                if (filmKey != null) {
                    // 1. Delete from Items (Top Movies)
                    database.getReference("Items").child(filmKey).removeValue()

                    // 2. Also try to delete from Upcoming/Upcomming in case it's duplicated there
                    database.getReference("Upcoming").child(filmKey).removeValue()
                    database.getReference("Upcomming").child(filmKey).removeValue()

                    // 3. Delete schedules associated with this movie title
                    film.Title?.let { title ->
                        database.getReference("Schedules").child(title).removeValue()
                    }

                    Toast.makeText(this, "Movie deleted from all sections", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
