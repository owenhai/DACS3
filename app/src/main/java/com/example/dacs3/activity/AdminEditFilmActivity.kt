package com.example.dacs3.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAdminEditFilmBinding
import com.example.dacs3.model.Film
import com.google.firebase.database.FirebaseDatabase

class AdminEditFilmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminEditFilmBinding
    private val database = FirebaseDatabase.getInstance()
    private var existingFilm: Film? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminEditFilmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        existingFilm = intent.getSerializableExtra("film") as? Film

        setupUI()
        binding.backBtn.setOnClickListener { finish() }
        binding.saveBtn.setOnClickListener { saveFilm() }
    }

    private fun setupUI() {
        if (existingFilm != null) {
            binding.titleTxt.text = "Edit Movie"
            binding.titleEdit.setText(existingFilm?.Title)
            binding.titleEdit.isEnabled = false // Title is the path key
            binding.descriptionEdit.setText(existingFilm?.Description)
            binding.posterEdit.setText(existingFilm?.Poster)
            binding.trailerEdit.setText(existingFilm?.Trailer)
            binding.imdbEdit.setText(existingFilm?.Imdb.toString())
            binding.yearEdit.setText(existingFilm?.Year.toString())
            binding.priceEdit.setText(existingFilm?.Price.toString())
            binding.timeEdit.setText(existingFilm?.Time)
            binding.genreEdit.setText(existingFilm?.Genre?.joinToString(", "))
        } else {
            binding.titleTxt.text = "Add New Movie"
        }
    }

    private fun saveFilm() {
        val title = binding.titleEdit.text.toString().trim()
        val desc = binding.descriptionEdit.text.toString().trim()
        val poster = binding.posterEdit.text.toString().trim()
        val trailer = binding.trailerEdit.text.toString().trim()
        val imdb = binding.imdbEdit.text.toString().toIntOrNull() ?: 0
        val year = binding.yearEdit.text.toString().toIntOrNull() ?: 0
        val price = binding.priceEdit.text.toString().toDoubleOrNull() ?: 0.0
        val time = binding.timeEdit.text.toString().trim()
        val genres = ArrayList(binding.genreEdit.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() })

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val film = Film(
            Title = title,
            Description = desc,
            Poster = poster,
            Trailer = trailer,
            Imdb = imdb,
            Year = year,
            Price = price,
            Time = time,
            Genre = genres
        )

        database.getReference("Items").child(title).setValue(film)
            .addOnSuccessListener {
                Toast.makeText(this, "Movie Saved Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
