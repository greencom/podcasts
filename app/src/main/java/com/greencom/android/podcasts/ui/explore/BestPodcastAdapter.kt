package com.greencom.android.podcasts.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.databinding.ItemBestPodcastBinding
import com.greencom.android.podcasts.utils.PodcastDiffCallback
import com.greencom.android.podcasts.utils.coverBuilder
import com.greencom.android.podcasts.utils.setupSubscribeToggleButton

/** Adapter used for RecyclerView that represents a list of best podcasts. */
class BestPodcastAdapter(
    private val navigateToPodcast: (String) -> Unit,
    private val updateSubscription: (String, Boolean) -> Unit
) : ListAdapter<PodcastShort, BestPodcastViewHolder>(PodcastDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BestPodcastViewHolder {
        return BestPodcastViewHolder.create(parent, navigateToPodcast, updateSubscription)
    }

    override fun onBindViewHolder(holder: BestPodcastViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast)
    }
}

/** ViewHolder that represents a single item in the best podcasts list. */
class BestPodcastViewHolder private constructor(
    private val binding: ItemBestPodcastBinding,
    private val navigateToPodcast: (String) -> Unit,
    private val updateSubscription: (String, Boolean) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val context = binding.root.context

    // Podcast associated with this ViewHolder.
    private lateinit var podcast: PodcastShort

    init {
        // Update subscription to the podcast.
        binding.subscribe.setOnClickListener {
            updateSubscription(podcast.id, (it as MaterialButton).isChecked)
            // Keep the button checked until the user makes his choice in the UnsubscribeDialog.
            if (podcast.subscribed) binding.subscribe.isChecked = true
        }

        // Navigate to PodcastFragment.
        binding.root.setOnClickListener {
            navigateToPodcast(podcast.id)
        }
    }

    /** Bind ViewHolder with a given [PodcastShort]. */
    fun bind(podcast: PodcastShort) {
        // Update the podcast associated with this ViewHolder.
        this.podcast = podcast

        binding.apply {
            cover.load(podcast.image) { coverBuilder(context) }
            title.text = podcast.title
            publisher.text = podcast.publisher
            explicitContent.isVisible = podcast.explicitContent
            setupSubscribeToggleButton(subscribe, podcast.subscribed, context)

            // Remove all HTML tags from description.
            description.text = HtmlCompat.fromHtml(
                podcast.description,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toString().trim()
        }
    }

    companion object {
        /** Create a [BestPodcastViewHolder]. */
        fun create(
            parent: ViewGroup,
            navigateToPodcast: (String) -> Unit,
            updateSubscription: (String, Boolean) -> Unit
        ): BestPodcastViewHolder {
            val binding = ItemBestPodcastBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return BestPodcastViewHolder(binding, navigateToPodcast, updateSubscription)
        }
    }
}