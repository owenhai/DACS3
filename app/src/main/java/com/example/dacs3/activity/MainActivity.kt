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
import com.bumptech.glide.Glide
import com.example.dacs3.R
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
        if (binding.viewPager2.adapter != null && binding.viewPager2.adapter!!.itemCount > 0) {
            binding.viewPager2.currentItem = binding.viewPager2.currentItem + 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
// ... (rest of the setup)

        // Check user session
        checkUserSession()

        // Load user profile
        loadUserProfile()

        // Check for Admin privileges
        checkAdminStatus()

        // Setup Search bar
        binding.editTextText.setOnEditorActionListener { v, actionId, event ->
            val query = binding.editTextText.text.toString().trim()
            if (query.isNotEmpty()) {
                startActivity(Intent(this, FilmsListActivity::class.java).apply {
                    putExtra("type", "Items")
                    putExtra("searchQuery", query)
                })
            }
            true
        }

        binding.editTextText.setOnClickListener {
            startActivity(Intent(this, FilmsListActivity::class.java).apply {
                putExtra("type", "Items")
            })
        }

        // Setup "See All" buttons
        binding.seeAllTopMovies.setOnClickListener {
            startActivity(Intent(this, FilmsListActivity::class.java).apply {
                putExtra("type", "Items")
            })
        }

        binding.seeAllUpcomingMovies.setOnClickListener {
            startActivity(Intent(this, FilmsListActivity::class.java).apply {
                putExtra("type", "Upcoming")
            })
        }

        // Setup Explore tab highlight
        binding.bottomNavigation.setItemSelected(R.id.explorer, true)
        binding.bottomNavigation.setOnItemSelectedListener { id ->
            when (id) {
                R.id.favourites -> {
                    startActivity(Intent(this, FilmsListActivity::class.java).apply {
                        putExtra("type", "Favorites")
                    })
                }
                R.id.cart -> {
                    startActivity(Intent(this, TicketListActivity::class.java))
                }
                R.id.explorer -> {
                    // Stay on Explore
                }
                R.id.profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
            }
        }

        // Setup UI
        initBanner()
        initTopMovies()
        initUpcoming()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.setItemSelected(R.id.explorer, true)
        loadUserProfile()
        sliderHandle.postDelayed(sliderRunnable, 5000)
    }

    override fun onPause() {
        super.onPause()
        sliderHandle.removeCallbacks(sliderRunnable)
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

    private fun checkAdminStatus() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val permission = sharedPref.getString("permission", "user")

        // adminBtn has been removed from layout, so we no longer need to update it here.
        // The Admin Dashboard can still be accessed via the Profile screen.
    }

    private fun loadUserProfile() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val customUserId = sharedPref.getString("customUserId", null)

        if (customUserId != null) {
            database.getReference("Users").child(customUserId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("username").value?.toString() ?: "User"
                    val permission = snapshot.child("permission").value?.toString() ?: "user"
                    val profileImage = snapshot.child("profileImage").value?.toString()

                    // Sync to SharedPreferences
                    sharedPref.edit().apply {
                        putString("username", username)
                        putString("permission", permission)
                        apply()
                    }

                    // Update UI
                    binding.textView3.text = "Hello $username"
                    checkAdminStatus()

                    if (!profileImage.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImage)
                            .placeholder(R.drawable.profile)
                            .circleCrop()
                            .into(binding.imageView2)
                    }
                }
            }.addOnFailureListener {
                // Fallback to local prefs if network fails
                val username = sharedPref.getString("username", "User")
                binding.textView3.text = "Hello $username"
            }
        }
    }

        private fun initTopMovies() {
            val myRef : DatabaseReference = database.getReference("Items")
            binding.progressBarTopMovies.visibility = View.VISIBLE

            myRef.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = ArrayList<Film>()
                    if(snapshot.exists()){
                        for (i in snapshot.children){
                            val item = i.getValue(Film::class.java)
                            if(item != null){
                                item.key = i.key
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
                            binding.recyclerViewTopMovies.adapter = FilmListAdapter(items, false)
                        } else {
                            // If list becomes empty, clear adapter
                            binding.recyclerViewTopMovies.adapter = FilmListAdapter(arrayListOf(), false)
                        }
                    }
                    binding.progressBarTopMovies.visibility = View.GONE
                }

                override fun onCancelled(p0: DatabaseError) {
                    binding.progressBarTopMovies.visibility = View.GONE
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
                    sliderHandle.postDelayed(sliderRunnable, 5000)
                }
            })
        }

        private fun initUpcoming() {
            loadUpcoming("Upcoming", fallbackToLegacy = true)
        }

        private fun loadUpcoming(path: String, fallbackToLegacy: Boolean) {
            val myRef: DatabaseReference = database.getReference(path)
            binding.progressBarUpcoming.visibility = View.VISIBLE

            myRef.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = ArrayList<Film>()
                    if (snapshot.exists()) {
                        for (i in snapshot.children){
                            val item = i.getValue(Film::class.java)
                            if(item != null){
                                item.key = i.key
                                items.add(item)
                            }
                        }
                    }

                    if (items.isNotEmpty()) {
                        binding.recyclerViewUpcoming.layoutManager =
                            LinearLayoutManager(
                                this@MainActivity,
                                LinearLayoutManager.HORIZONTAL,
                                false
                            )
                        binding.recyclerViewUpcoming.adapter = FilmListAdapter(items, false)
                        binding.progressBarUpcoming.visibility = View.GONE
                    } else {
                        if (fallbackToLegacy && path == "Upcoming") {
                            loadUpcoming("Upcomming", fallbackToLegacy = false)
                        } else {
                            binding.recyclerViewUpcoming.adapter = FilmListAdapter(arrayListOf(), false)
                            binding.progressBarUpcoming.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError){
                    binding.progressBarUpcoming.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Upcoming load failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }




