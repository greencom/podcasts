package com.greencom.android.podcasts.ui.explore

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.PodcastItemBinding
import com.greencom.android.podcasts.utils.PodcastDiffCallback

/**
 * Adapter used for RecyclerView that represents a list of best podcasts.
 *
 * @param updateSubscription function used to update a subscription on the podcast.
 * @param onPodcastClick function used to navigate to the podcast page on click.
 */
class ExplorePodcastAdapter(
    private val updateSubscription: (Podcast, Boolean) -> Unit,
    private val onPodcastClick: (Podcast) -> Unit,
) : ListAdapter<Podcast, ExplorePodcastViewHolder>(PodcastDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExplorePodcastViewHolder {
        return ExplorePodcastViewHolder.create(parent, updateSubscription, onPodcastClick)
    }

    override fun onBindViewHolder(holder: ExplorePodcastViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast)
    }
}

/** ViewHolder that represents a single item in the best podcasts list. */
class ExplorePodcastViewHolder private constructor(
    private val binding: PodcastItemBinding,
    private val updateSubscription: (Podcast, Boolean) -> Unit,
    private val onPodcastClick: (Podcast) -> Unit,
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
        binding.cover.load(podcast.image)
        binding.title.text = podcast.title
        binding.publisher.text = podcast.publisher
        // Remove all HTML tags from description.
        binding.description.text =
            HtmlCompat.fromHtml(podcast.description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        // Change `Subscribe` button state depending on `inSubscription` value.
        binding.subscribe.isChecked = podcast.subscribed
        updateSubscribeButton(podcast.subscribed)
        // Show explicit content icon depending on `explicitContent` value.
        binding.explicitContent.isVisible = podcast.explicitContent
    }

    /** Set listeners for item's content. */
    private fun setListeners(podcast: Podcast) {
        // Navigate to PodcastFragment on podcast click.
        binding.root.setOnClickListener {
            onPodcastClick(podcast)
        }

        // `Subscribe` button onClickListener.
        binding.subscribe.setOnClickListener {
            updateSubscription(podcast, binding.subscribe.isChecked)
        }

        // `Subscribe` button onCheckedChangeListener.
        binding.subscribe.addOnCheckedChangeListener { _, isChecked ->
            updateSubscribeButton(isChecked)
        }
    }

    /** Update the `Subscribe` button depending on whether it is checked. */
    private fun updateSubscribeButton(isChecked: Boolean) {
        if (isChecked) {
            binding.subscribe.icon = ContextCompat.getDrawable(context, R.drawable.ic_check_24)
            binding.subscribe.text = context.getString(R.string.explore_subscribed)
        } else {
            binding.subscribe.icon = ContextCompat.getDrawable(context, R.drawable.ic_add_24)
            binding.subscribe.text = context.getString(R.string.explore_subscribe)
        }
    }

    companion object {
        /**
         * Create a [ExplorePodcastViewHolder].
         *
         * @param parent parent's ViewGroup.
         * @param updateSubscription function used to update a subscription on the podcast.
         * @param onPodcastClick function used to navigate to the podcast page on click.
         */
        fun create(
            parent: ViewGroup,
            updateSubscription: (Podcast, Boolean) -> Unit,
            onPodcastClick: (Podcast) -> Unit,
        ): ExplorePodcastViewHolder {
            val binding = PodcastItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return ExplorePodcastViewHolder(binding, updateSubscription, onPodcastClick)
        }
    }
}