package com.greencom.android.podcasts.ui.explore

import android.content.Context
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
import com.greencom.android.podcasts.utils.PodcastShortDiffCallback
import com.greencom.android.podcasts.utils.coilCoverBuilder
import com.greencom.android.podcasts.utils.setupSubscribeToggleButton

/** Adapter used for RecyclerView that represents a list of best podcasts. */
class BestPodcastAdapter(
    private val navigateToPodcast: (String) -> Unit,
    private val updateSubscription: (String, Boolean) -> Unit
) : ListAdapter<PodcastShort, BestPodcastAdapter.ViewHolder>(PodcastShortDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(parent, navigateToPodcast, updateSubscription)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcast = getItem(position)
        val isLast = position == itemCount - 1
        holder.bind(podcast, isLast)
    }

    /** ViewHolder that represents a single item in the best podcasts list. */
    class ViewHolder private constructor(
        private val binding: ItemBestPodcastBinding,
        private val navigateToPodcast: (String) -> Unit,
        private val updateSubscription: (String, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context: Context
            get() = binding.root.context

        /** Podcast associated with this ViewHolder. */
        private lateinit var podcast: PodcastShort

        init {
            // Update subscription to the podcast.
            binding.subscribe.setOnClickListener {
                updateSubscription(podcast.id, (it as MaterialButton).isChecked)
                // Keep the button checked until the user makes his choice in the UnsubscribeDialog.
                if (podcast.subscribed) {
                    binding.subscribe.isChecked = true
                }
            }

            // Navigate to PodcastFragment.
            binding.root.setOnClickListener {
                navigateToPodcast(podcast.id)
            }
        }

        /** Bind ViewHolder with a given [PodcastShort]. */
        fun bind(podcast: PodcastShort, isLast: Boolean) {
            // Update the podcast associated with this ViewHolder.
            this.podcast = podcast

            binding.apply {
                cover.load(podcast.image) { coilCoverBuilder(context) }
                title.text = podcast.title
                publisher.text = podcast.publisher
                explicitContent.isVisible = podcast.explicitContent
                setupSubscribeToggleButton(subscribe, podcast.subscribed, context)

                // Remove all HTML tags from description.
                description.text = HtmlCompat.fromHtml(
                    podcast.description,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString().trim()

                divider.isVisible = !isLast
            }
        }

        companion object {
            /** Create a [ViewHolder]. */
            fun create(
                parent: ViewGroup,
                navigateToPodcast: (String) -> Unit,
                updateSubscription: (String, Boolean) -> Unit
            ): ViewHolder {
                val binding = ItemBestPodcastBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(binding, navigateToPodcast, updateSubscription)
            }
        }
    }
}