package com.example.dacs3.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth
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
        firebaseAuth = FirebaseAuth.getInstance()

        // Check user session
        checkUserSession()

        // Load user profile
        loadUserProfile()


        // Setup logout button
        binding.logoutBtn.setOnClickListener {
            logoutUser()
        }

        // Setup UI
        initBanner()
        initTopMovies()
        initUpcoming()
    }

    private fun checkUserSession() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val firebaseUid = sharedPref.getString("firebaseUid", null)

        if (firebaseUid == null) {
            // User not logged in, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadUserProfile() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User")

        // Handle "null" string case
        val displayName = if (username.isNullOrEmpty() || username == "null") "User" else username
        binding.textView3.text = "Hello $displayName"
    }

    private fun logoutUser() {
        // Sign out from Firebase
        firebaseAuth.signOut()

        // Clear SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            clear()
            apply()
        }

        // Go to LoginActivity
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }


        private fun initTopMovies() {
            val myRef : DatabaseReference = database.getReference("Items")
            binding.progressBarTopMovies.visibility = View.VISIBLE
            val items = ArrayList<Film>()

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
                    }
                    binding.progressBarTopMovies.visibility = View.GONE
                }

                override fun onCancelled(p0: DatabaseError) {

                }

            })
        }

        private fun initBanner() {
            val myRef = database.getReference("Banners")
            binding.progressBarSlider.visibility = View.VISIBLE

            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<SliderItems>()
                    for (data in snapshot.children) {
                        val item = data.getValue(SliderItems::class.java)
                        item?.let { list.add(it) }
                    }
                    banners(list)
                    binding.progressBarSlider.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
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

        private fun initUpcoming() {
            val myRef : DatabaseReference = database.getReference("Upcoming")
            binding.progressBarUpcoming.visibility = View.VISIBLE
            val items = ArrayList<Film>()

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
                            binding.recyclerViewUpcoming.layoutManager =
                                LinearLayoutManager(
                                    this@MainActivity,
                                    LinearLayoutManager.HORIZONTAL,
                                    false
                                )
                            binding.recyclerViewUpcoming.adapter = FilmListAdapter(items)
                        }
                    }
                    binding.progressBarUpcoming.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError){

                }

            })
        }
    }

