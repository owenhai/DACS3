package com.example.dacs3.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dacs3.databinding.ViewholderCastBinding
import com.example.dacs3.databinding.ViewholderFilmBinding
import com.example.dacs3.model.Cast


class CastListAdapter(private val cast : List<Cast>) :
    RecyclerView.Adapter<CastListAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ViewholderCastBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cast: Cast) {
            Glide.with(itemView.context)
                .load(cast.PicUrl)
                .into(binding.actorImage)

            binding.nameTxt.text = cast.Actor
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CastListAdapter.ViewHolder {
        val binding = ViewholderCastBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CastListAdapter.ViewHolder, postion: Int) {
        holder.bind(cast[postion])
    }

    override fun getItemCount(): Int = cast.size }

