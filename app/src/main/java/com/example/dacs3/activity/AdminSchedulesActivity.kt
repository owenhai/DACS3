package com.example.dacs3.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.activity.AdminMovieSchedulesActivity
import com.example.dacs3.adapter.AdminFilmAdapter
import com.example.dacs3.databinding.ActivityAdminSchedulesBinding
import com.example.dacs3.model.Film
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminSchedulesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminSchedulesBinding
    private val database = FirebaseDatabase.getInstance()
    private val allMovies = mutableListOf<Film>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminSchedulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener { finish() }
        setupRecyclerView()
        loadMovies()
    }

    private fun setupRecyclerView() {
        binding.movieListRecyclerView.layoutManager = LinearLayoutManager(this)
        // reuse adapter, edit click opens schedule manager
        binding.movieListRecyclerView.adapter = AdminFilmAdapter(allMovies,
            onEditClick = { film ->
                val intent = Intent(this, AdminMovieSchedulesActivity::class.java)
                intent.putExtra("film", film)
                startActivity(intent)
            },
            onDeleteClick = {}
        )
    }

    private fun loadMovies() {
        database.getReference("Items").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMovies.clear()
                for (child in snapshot.children) {
                    val film = child.getValue(Film::class.java)
                    film?.let { allMovies.add(it) }
                }
                binding.movieListRecyclerView.adapter?.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
