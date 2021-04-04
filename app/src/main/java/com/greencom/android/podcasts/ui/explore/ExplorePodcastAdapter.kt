package com.greencom.android.podcasts.ui.explore

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.PodcastItemBinding
import com.greencom.android.podcasts.utils.PodcastDiffCallback

/** Adapter used for RecyclerView that represents a list of best podcasts. */
class ExplorePodcastAdapter :
    ListAdapter<Podcast, ExplorePodcastViewHolder>(PodcastDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExplorePodcastViewHolder {
        return ExplorePodcastViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ExplorePodcastViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast)
    }
}

/** ViewHolder that represents a single item in the best podcasts list. */
class ExplorePodcastViewHolder private constructor(
    private val binding: PodcastItemBinding,
) : RecyclerView.ViewHolder(binding.root) {

    // View context.
    private val context: Context = binding.root.context

    /** Bind ViewHolder with a given [Podcast]. */
    fun bind(podcast: Podcast) {
        binding.cover.load(podcast.image) {
            crossfade(true)
            placeholder(R.drawable.shape_placeholder)
            transformations(RoundedCornersTransformation(
                context.resources.getDimension(R.dimen.corner_radius_small)
            ))
        }
        binding.title.text = podcast.title
        binding.publisher.text = podcast.publisher
        // Remove all HTML tags from description.
        binding.description.text =
            HtmlCompat.fromHtml(podcast.description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        // Show explicit content icon depending on `explicitContent` value.
        binding.explicitContent.isVisible = podcast.explicitContent
    }

    companion object {
        /**
         * Create a [ExplorePodcastViewHolder].
         *
         * @param parent parent's ViewGroup.
         */
        fun create(parent: ViewGroup): ExplorePodcastViewHolder {
            val binding = PodcastItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return ExplorePodcastViewHolder(binding)
        }
    }
}