package com.greencom.android.podcasts.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.PodcastItemBinding

/** TODO: Documentation */
class ExplorePodcastAdapter :
    PagingDataAdapter<Podcast, ExplorePodcastViewHolder>(ExplorePodcastDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExplorePodcastViewHolder {
        return ExplorePodcastViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ExplorePodcastViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }
}

/** TODO: Documentation */
class ExplorePodcastViewHolder private constructor(private val binding: PodcastItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    /** TODO: Documentation */
    fun bind(item: Podcast) {
        Glide.with(binding.cover.context)
            .load(item.image)
            .into(binding.cover)
        binding.title.text = item.title
        binding.publisher.text = item.publisher
        binding.description.text =
            HtmlCompat.fromHtml(item.description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        if (item.explicitContent) {
            binding.explicitContent.visibility = View.VISIBLE
        } else {
            binding.explicitContent.visibility = View.INVISIBLE
        }
        binding.explicitContent.isVisible = item.explicitContent
    }

    companion object {
        /** TODO: Documentation */
        fun create(parent: ViewGroup): ExplorePodcastViewHolder {
            val binding = PodcastItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return ExplorePodcastViewHolder(binding)
        }
    }
}

/** TODO: Documentation */
class ExplorePodcastDiffCallback : DiffUtil.ItemCallback<Podcast>() {

    override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem == newItem
    }
}