package com.greencom.android.podcasts.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.PodcastItemBinding

/** TODO: Documentation */
class ExplorePodcastAdapter : PagingDataAdapter<Podcast, PodcastViewHolder>(PodcastDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastViewHolder {
        return PodcastViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }
}

/** TODO: Documentation */
class PodcastViewHolder private constructor(private val binding: PodcastItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    /** TODO: Documentation */
    fun bind(item: Podcast) {
        binding.title.text = item.title
        binding.publisher.text = item.publisher
        binding.description.text = item.description
        if (item.explicitContent) {
            binding.explicitContent.visibility = View.VISIBLE
        } else {
            binding.explicitContent.visibility = View.INVISIBLE
        }
        Glide.with(binding.cover.context)
            .load(item.image)
            .into(binding.cover)
    }

    companion object {
        /** TODO: Documentation */
        fun from(parent: ViewGroup): PodcastViewHolder {
            val binding = PodcastItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return PodcastViewHolder(binding)
        }
    }
}

/** TODO: Documentation */
class PodcastDiffCallback : DiffUtil.ItemCallback<Podcast>() {

    override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem == newItem
    }
}