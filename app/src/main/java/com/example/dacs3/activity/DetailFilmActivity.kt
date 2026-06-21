package com.example.dacs3.activity

import android.content.Intent
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.renderscript.RenderScript
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.dacs3.R
import com.example.dacs3.adapter.CastListAdapter
import com.example.dacs3.databinding.ActivityDetailFilmBinding
import eightbitlab.com.blurview.RenderScriptBlur
import com.example.dacs3.model.Film
import com.google.firebase.database.FirebaseDatabase
import android.widget.Toast

class DetailFilmActivity : AppCompatActivity() {
    private lateinit var binding: com.example.dacs3.databinding.ActivityDetailFilmBinding
    private val database = FirebaseDatabase.getInstance()
    private var isFavourite = false
    private var favouriteKey = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailFilmBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setVariable()
    }

    private fun setVariable() {
        val film = intent.getSerializableExtra("object") as com.example.dacs3.model.Film
        val requestOptions = RequestOptions().transform(CenterCrop(),
            GranularRoundedCorners(0f,0f,50f,50f))

        Glide.with(this)
            .load(film.Poster)
            .apply(requestOptions)
            .into(binding.filmPic)

        binding.titleTxt.text = film.Title
        binding.imdbTxt.text = "IMDB: ${film.Imdb}"
        binding.movieTimeTxt.text = "${film.Year} - ${film.Time}"
        binding.movieSummeryTxt.text = film.Description

        binding.backBtn.setOnClickListener { finish() }

        val radius = 10f
        val decorView = window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val windowBackGround = decorView.background

        binding.blurView.setupWith(rootView, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackGround)
            .setBlurRadius(radius)
        binding.blurView.outlineProvider = ViewOutlineProvider.BACKGROUND
        binding.blurView.clipToOutline = true


        film.Genre?.let {
            binding.genreView.adapter = com.example.dacs3.adapter.GenreEachFilmAdapter(it)
            binding.genreView.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(
                    this,
                    androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                    false)
        }

        film.Casts?.let {
            binding.castListView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false)
            binding.castListView.adapter = CastListAdapter(it)
        }

        binding.buyTicketBtn.setOnClickListener {
            val intent = Intent(this, SeatListActivity::class.java)
            intent.putExtra("film", film)
            startActivity(intent)
        }

        // Add listener to play Trailer when clicking on movie poster
        binding.filmPic.setOnClickListener {
            val trailerUrl = film.Trailer
            if (!trailerUrl.isNullOrEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trailerUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open Trailer link", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Trailer not available for this movie", Toast.LENGTH_SHORT).show()
            }
        }

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val customUserId = sharedPref.getString("customUserId", null)
        val favouritesRef = customUserId?.let {
            database.getReference("Users").child(it).child("favorites")
        }

        favouriteKey = buildFavoriteKey(film)
        favouritesRef?.child(favouriteKey)?.get()?.addOnSuccessListener { snapshot ->
            isFavourite = snapshot.exists()
            updateBookmarkIcon()
        }

        binding.bookmarkBtn.setOnClickListener {
            if (favouritesRef == null) {
                Toast.makeText(this, "Please login to use favourites", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isFavourite) {
                favouritesRef.child(favouriteKey).removeValue().addOnSuccessListener {
                    isFavourite = false
                    updateBookmarkIcon()
                    Toast.makeText(this, "Removed from favourites", Toast.LENGTH_SHORT).show()
                }
            } else {
                favouritesRef.child(favouriteKey).setValue(film).addOnSuccessListener {
                    isFavourite = true
                    updateBookmarkIcon()
                    Toast.makeText(this, "Added to favourites", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBookmarkIcon() {
        if (isFavourite) {
            binding.bookmarkBtn.setColorFilter(Color.YELLOW)
        } else {
            binding.bookmarkBtn.clearColorFilter()
        }
    }

    private fun buildFavoriteKey(film: Film): String {
        val raw = film.Title ?: film.Poster ?: "film"
        return raw.replace(Regex("[.#$\\[\\]]"), "_")
    }
}