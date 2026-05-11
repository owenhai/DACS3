    package com.example.dacs3

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.dacs3.adapter.FilmListAdapter
import com.example.dacs3.adapter.SliderAdapter
import com.example.dacs3.databinding.ActivityMainBinding
import com.example.dacs3.model.Film
import com.example.dacs3.model.SliderItems
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.abs

    class MainActivity : AppCompatActivity() {

        private lateinit var binding: ActivityMainBinding
        private lateinit var database: FirebaseDatabase
        private val sliderHandle = Handler(Looper.getMainLooper())
        private val sliderRunnable = Runnable {
            binding.viewPager2.currentItem = binding.viewPager2.currentItem + 1
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            enableEdgeToEdge()
            setContentView(binding.root)

            database = FirebaseDatabase.getInstance()

            initBanner()
            initTopMovies()
        }

        private fun initTopMovies() {
            val myRef : DatabaseReference = database.getReference("Items")
            binding.progressBarTopMovies.visibility = View.VISIBLE
            val items = ArrayList<com.example.dacs3.model.Film>()

            myRef.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        for (i in snapshot.children){
                            val item = i.getValue(Film::class.java)
                            if(item != null){
                                items.add(item)
                            }
                        }
                        if(items.isNotEmpty()){
                            binding.recyclerViewTopMovies.layoutManager =
                                LinearLayoutManager(
                                    this@MainActivity,
                                    LinearLayoutManager.HORIZONTAL,
                                    false
                                )
                            binding.recyclerViewTopMovies.adapter = FilmListAdapter(items)
                        }
                        binding.progressBarTopMovies.visibility = View.GONE
                    }
                }

                override fun onCancelled(p0: DatabaseError) {

                }

            })
        }

        private fun initBanner() {
            val myRef = database.getReference("Banners")
            binding.progressBarSlider.visibility = View.VISIBLE

            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val list = mutableListOf<SliderItems>()
                    for (data in snapshot.children) {
                        val item = data.getValue(SliderItems::class.java)
                        item?.let { list.add(it) }
                    }
                    banners(list)
                    binding.progressBarSlider.visibility = View.GONE
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                }
            })
        }

        private fun banners(list: MutableList<SliderItems>) {
            binding.viewPager2.adapter = SliderAdapter(list, binding.viewPager2)
            binding.viewPager2.clipToPadding = false
            binding.viewPager2.clipChildren = false
            binding.viewPager2.offscreenPageLimit = 3

            val compositePageTransformer = CompositePageTransformer().apply {
                addTransformer(MarginPageTransformer(40))
                addTransformer { page, position ->
                    val r = 1 - abs(position)
                    page.scaleY = 0.85f + r * 0.15f
                }
            }
            binding.viewPager2.setPageTransformer(compositePageTransformer)
            binding.viewPager2.currentItem = 1
            binding.viewPager2.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    sliderHandle.removeCallbacks(sliderRunnable)
                }
            })
        }
    }