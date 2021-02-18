package com.greencom.android.podcasts.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.PodcastItemBinding

class ExplorePodcastAdapter : RecyclerView.Adapter<PodcastViewHolder>() {

    var data = listOf<Podcast>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        return PodcastViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val item = data[position]

        holder.bind(item)
    }

    override fun getItemCount(): Int = data.size
}

class PodcastViewHolder private constructor(private val binding: PodcastItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Podcast) {
        binding.title.text = item.title
        binding.publisher.text = item.publisher
        binding.description.text = item.description
        if (item.explicitContent) {
            binding.explicitContent.visibility = View.VISIBLE
        } else {
            binding.explicitContent.visibility = View.INVISIBLE
        }
    }

    companion object {
        fun from(parent: ViewGroup): PodcastViewHolder {
            val binding = PodcastItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return PodcastViewHolder(binding)
        }
    }
}