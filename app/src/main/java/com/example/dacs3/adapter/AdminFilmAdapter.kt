package com.example.dacs3.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dacs3.R
import com.example.dacs3.databinding.ItemAdminFilmBinding
import com.example.dacs3.model.Film

class AdminFilmAdapter(
    private var films: MutableList<Film>,
    private val onEditClick: (Film) -> Unit,
    private val onDeleteClick: (Film) -> Unit
) : RecyclerView.Adapter<AdminFilmAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminFilmBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminFilmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val film = films[position]
        holder.binding.titleTxt.text = film.Title
        holder.binding.infoTxt.text = "${film.Genre.joinToString(", ")} | ${film.Year}"

        Glide.with(holder.itemView.context)
            .load(film.Poster)
            .placeholder(R.drawable.login_pic)
            .into(holder.binding.posterImg)

        holder.binding.editBtn.setOnClickListener { onEditClick(film) }
        holder.binding.deleteBtn.setOnClickListener { onDeleteClick(film) }
    }

    override fun getItemCount(): Int = films.size

    fun updateData(newFilms: List<Film>) {
        films.clear()
        films.addAll(newFilms)
        notifyDataSetChanged()
    }
}
