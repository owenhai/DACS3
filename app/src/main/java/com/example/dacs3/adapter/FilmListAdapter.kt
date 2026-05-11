package com.example.dacs3.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.model.Film
import android.content.Context
import android.view.LayoutInflater
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.dacs3.databinding.ViewholderFilmBinding

class FilmListAdapter (private val items : ArrayList<Film>) :
RecyclerView.Adapter<FilmListAdapter.ViewHolder>(){

    private var context : Context? = null
    inner class ViewHolder (private val binding : ViewholderFilmBinding) :
    RecyclerView.ViewHolder(binding.root){
        fun bind(item : Film){
           binding.nameTxt.text = item.Title
            val requestOptions = RequestOptions()
                .transform(CenterCrop(), RoundedCorners(30))

            Glide.with(context!!)
                .load(item.Poster)
                .apply(requestOptions)
                .into(binding.pic)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FilmListAdapter.ViewHolder {
        context = parent.context
        val binding = ViewholderFilmBinding.inflate(
            LayoutInflater.from(context), parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilmListAdapter.ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() : Int = items.size
}