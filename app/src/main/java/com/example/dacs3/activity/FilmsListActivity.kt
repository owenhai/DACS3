package com.example.dacs3.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dacs3.adapter.FilmListAdapter
import com.example.dacs3.databinding.ActivityFilmsListBinding
import com.example.dacs3.model.Film
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FilmsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilmsListBinding
    private val database = FirebaseDatabase.getInstance()
    private val allItems = ArrayList<Film>()
    private lateinit var adapter: FilmListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilmsListBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val type = intent.getStringExtra("type") ?: "Items"
        val searchQuery = intent.getStringExtra("searchQuery")
        
        setupRecyclerView()
        setupHeader(type)
        setupSearch()
        loadFilms(type, searchQuery)
    }

    private fun setupHeader(type: String) {
        binding.titleFilmsList.text = when (type) {
            "Upcoming" -> "Upcoming Movies"
            "Favorites" -> "Favourite Movies"
            else -> "Top Movies"
        }
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = FilmListAdapter(ArrayList())
        binding.recyclerViewFilmsList.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewFilmsList.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No-op
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFilms(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {
                // No-op
            }
        })
    }

    private fun filterFilms(query: String) {
        val keyword = query.trim().lowercase()
        if (keyword.isEmpty()) {
            adapter.updateItems(allItems)
            return
        }
        val filtered = allItems.filter { film ->
            film.Title?.lowercase()?.contains(keyword) == true
        }
        adapter.updateItems(filtered)
    }

    private fun loadFilms(type: String, searchQuery: String? = null) {
        binding.progressBarFilmsList.visibility = View.VISIBLE
        allItems.clear()

        if (type == "Favorites") {
            loadFavorites(searchQuery)
            return
        }

        database.getReference(type)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allItems.clear()
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val item = child.getValue(Film::class.java)
                            if (item != null) {
                                item.key = child.key
                                allItems.add(item)
                            }
                        }
                    }
                    
                    if (searchQuery != null) {
                        binding.searchEditText.setText(searchQuery)
                        filterFilms(searchQuery)
                    } else {
                        adapter.updateItems(allItems)
                    }
                    binding.progressBarFilmsList.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBarFilmsList.visibility = View.GONE
                    Toast.makeText(this@FilmsListActivity, "Load failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadFavorites(searchQuery: String? = null) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val customUserId = sharedPref.getString("customUserId", null)

        if (customUserId.isNullOrEmpty()) {
            binding.progressBarFilmsList.visibility = View.GONE
            Toast.makeText(this, "Please login to view favourites", Toast.LENGTH_SHORT).show()
            return
        }

        database.getReference("Users").child(customUserId).child("favorites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allItems.clear()
                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val item = child.getValue(Film::class.java)
                            if (item != null) {
                                allItems.add(item)
                            }
                        }
                    }
                    
                    if (searchQuery != null) {
                        binding.searchEditText.setText(searchQuery)
                        filterFilms(searchQuery)
                    } else {
                        adapter.updateItems(allItems)
                    }
                    binding.progressBarFilmsList.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBarFilmsList.visibility = View.GONE
                    Toast.makeText(this@FilmsListActivity, "Load failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
