package com.example.dacs3.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.dacs3.databinding.ActivityAdminEditFilmBinding
import com.example.dacs3.model.Film
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class AdminEditFilmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminEditFilmBinding
    private val database = FirebaseDatabase.getInstance()
    private var existingFilm: Film? = null

    // Đã cập nhật API Key của bạn
    private val TMDB_API_KEY = "3ef6076d9e017db200b46d51f700f5c0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminEditFilmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        existingFilm = intent.getSerializableExtra("film") as? Film

        setupUI()
        binding.backBtn.setOnClickListener { finish() }
        binding.saveBtn.setOnClickListener { saveFilm() }

        binding.aiFillBtn.setOnClickListener {
            val title = binding.titleEdit.text.toString().trim()
            if (title.isNotEmpty()) {
                fetchMovieInfoFromTMDB(title)
            } else {
                Toast.makeText(this, "Please enter a movie title first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMovieInfoFromTMDB(title: String) {
        if (TMDB_API_KEY == "YOUR_TMDB_API_KEY") {
            Toast.makeText(this, "Please set your TMDB API Key in AdminEditFilmActivity", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "AI is searching for $title...", Toast.LENGTH_SHORT).show()

        thread {
            try {
                // Search for the movie to get ID
                val searchUrl = "https://api.themoviedb.org/3/search/movie?api_key=$TMDB_API_KEY&query=${title.replace(" ", "+")}"
                val searchResponse = URL(searchUrl).readText()
                val searchJson = JSONObject(searchResponse)
                val results = searchJson.getJSONArray("results")

                if (results.length() > 0) {
                    val firstMatch = results.getJSONObject(0)
                    val movieId = firstMatch.getInt("id")

                    // Fetch full details (duration, etc.)
                    val detailsUrl = "https://api.themoviedb.org/3/movie/$movieId?api_key=$TMDB_API_KEY"
                    val detailsResponse = URL(detailsUrl).readText()
                    val movieDetails = JSONObject(detailsResponse)

                    runOnUiThread {
                        populateFields(movieDetails)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "No movie found with that title", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateFields(json: JSONObject) {
        binding.descriptionEdit.setText(json.optString("overview", ""))

        val posterPath = json.optString("poster_path", "")
        if (posterPath.isNotEmpty() && posterPath != "null") {
            binding.posterEdit.setText("https://image.tmdb.org/t/p/w500$posterPath")
        }

        binding.imdbEdit.setText(json.optDouble("vote_average", 0.0).toString())

        val releaseDate = json.optString("release_date", "")
        if (releaseDate.length >= 4) {
            binding.yearEdit.setText(releaseDate.substring(0, 4))
        }

        val runtime = json.optInt("runtime", 0)
        if (runtime > 0) {
            binding.timeEdit.setText("$runtime min")
        }

        val genresArray = json.optJSONArray("genres")
        if (genresArray != null) {
            val genreList = mutableListOf<String>()
            for (i in 0 until genresArray.length()) {
                genreList.add(genresArray.getJSONObject(i).getString("name"))
            }
            binding.genreEdit.setText(genreList.joinToString(", "))
        }

        Toast.makeText(this, "AI Success: Found ${json.getString("title")}", Toast.LENGTH_SHORT).show()
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
        val imdb = binding.imdbEdit.text.toString().toDoubleOrNull() ?: 0.0
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
