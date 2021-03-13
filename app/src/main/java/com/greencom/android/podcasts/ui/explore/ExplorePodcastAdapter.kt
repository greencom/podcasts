package com.greencom.android.podcasts.ui.explore

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.PodcastItemBinding

/** Adapter used for RecyclerView that represents a list of best podcasts. */
class ExplorePodcastAdapter(
    private val updateSubscription: (Podcast) -> Unit
) : ListAdapter<Podcast, ExplorePodcastViewHolder>(ExplorePodcastDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExplorePodcastViewHolder {
        return ExplorePodcastViewHolder.create(parent, updateSubscription)
    }

    override fun onBindViewHolder(holder: ExplorePodcastViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast)
    }
}

/** ViewHolder that represents a single item in the best podcasts list. */
class ExplorePodcastViewHolder private constructor(
    private val binding: PodcastItemBinding,
    private val updateSubscription: (Podcast) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    // View context.
    private val context: Context = binding.root.context

    /** Bind ViewHolder with a given [Podcast]. */
    fun bind(podcast: Podcast) {
        fillView(podcast)
        setListeners(podcast)
    }

    /** Fill item view depending on podcast's properties. */
    private fun fillView(podcast: Podcast) {
        Glide.with(binding.cover.context)
            .load(podcast.image)
            .into(binding.cover)
        binding.title.text = podcast.title
        binding.publisher.text = podcast.publisher
        // Remove all HTML tags from description.
        binding.description.text =
            HtmlCompat.fromHtml(podcast.description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        // Change `Subscribe` button state depending on `inSubscription` value.
        if (podcast.inSubscriptions) {
            binding.subscribe.isChecked = true
            binding.subscribe.icon = ContextCompat.getDrawable(context, R.drawable.ic_check_24)
            binding.subscribe.text = context.getString(R.string.subscribed)
        }
        // Show explicit content icon depending on `explicitContent` value.
        binding.explicitContent.isVisible = podcast.explicitContent
    }

    /** Set listeners for item's content. */
    private fun setListeners(podcast: Podcast) {
        // `Subscribe` button onClickListener.
        binding.subscribe.setOnClickListener {
            updateSubscription(podcast)
        }

        // `Subscribe` button onCheckedChangeListener.
        binding.subscribe.addOnCheckedChangeListener { button, isChecked ->
            if (isChecked) {
                button.icon = ContextCompat.getDrawable(context, R.drawable.ic_check_24)
                button.text = context.getString(R.string.subscribed)
            } else {
                button.icon = ContextCompat.getDrawable(context, R.drawable.ic_add_24)
                button.text = context.getString(R.string.subscribe)
            }
        }
    }

    companion object {
        /** Create a [ExplorePodcastViewHolder]. */
        fun create(
            parent: ViewGroup,
            updateSubscription: (Podcast) -> Unit,
        ): ExplorePodcastViewHolder {
            val binding = PodcastItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return ExplorePodcastViewHolder(binding, updateSubscription)
        }
    }
}

class ExplorePodcastDiffCallback : DiffUtil.ItemCallback<Podcast>() {
    override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem == newItem
    }
}