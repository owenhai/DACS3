package com.example.dacs3.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ViewholderFilmBinding
import com.example.dacs3.databinding.ViewholderGenreBinding
import java.text.FieldPosition

class GenreEachFilmAdapter(private val items : List<String>) :
    RecyclerView.Adapter<GenreEachFilmAdapter.ViewHolder>() {
    class ViewHolder( val binding : ViewholderGenreBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GenreEachFilmAdapter.ViewHolder {
        val binding = ViewholderGenreBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenreEachFilmAdapter.ViewHolder, position: Int) {
        holder.binding.titleTxt.text = items[position]
    }

    override fun getItemCount(): Int = items.size
}