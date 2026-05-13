package com.example.dacs3.activity

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

class DetailFilmActivity : AppCompatActivity() {
    private lateinit var binding: com.example.dacs3.databinding.ActivityDetailFilmBinding

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
    }
}