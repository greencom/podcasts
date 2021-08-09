package com.greencom.android.podcasts.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.databinding.ItemSubscriptionsPodcastBinding
import com.greencom.android.podcasts.utils.PodcastShortDiffCallback

/** Adapter used for RecyclerView that represents a grid list of subscriptions. */
class SubscriptionsPodcastAdapter(
    private val navigateToPodcast: (String) -> Unit,
    private val showUnsubscribeDialog: (String) -> Unit,
) : ListAdapter<PodcastShort, SubscriptionsPodcastViewHolder>(PodcastShortDiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubscriptionsPodcastViewHolder {
        return SubscriptionsPodcastViewHolder.create(
            parent = parent,
            navigateToPodcast = navigateToPodcast,
            showUnsubscribeDialog = showUnsubscribeDialog
        )
    }

    override fun onBindViewHolder(holder: SubscriptionsPodcastViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast)
    }
}

/** ViewHolder that represents a single item in the subscriptions grid list. */
class SubscriptionsPodcastViewHolder private constructor(
    private val binding: ItemSubscriptionsPodcastBinding,
    private val navigateToPodcast: (String) -> Unit,
    private val showUnsubscribeDialog: (String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val context: Context
        get() = binding.root.context

    /** Podcast associated with this ViewHolder. */
    private lateinit var podcast: PodcastShort

    init {
        // Navigate to a podcast page.
        binding.cover.setOnClickListener {
            navigateToPodcast(podcast.id)
        }

        // Show an UnsubscribeDialog.
        binding.cover.setOnLongClickListener {
            showUnsubscribeDialog(podcast.id)
            true
        }
    }

    /** Bind ViewHOlder with a given [PodcastShort]. */
    fun bind(podcast: PodcastShort) {
        this.podcast = podcast

        binding.apply {
            cover.load(podcast.image) {
                crossfade(true)
                placeholder(R.drawable.shape_placeholder)
                error(R.drawable.shape_placeholder)
            }
            cover.contentDescription = podcast.title
        }
    }

    companion object {
        /** Create a [SubscriptionsPodcastViewHolder]. */
        fun create(
            parent: ViewGroup,
            navigateToPodcast: (String) -> Unit,
            showUnsubscribeDialog: (String) -> Unit,
        ): SubscriptionsPodcastViewHolder {
            val binding = ItemSubscriptionsPodcastBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return SubscriptionsPodcastViewHolder(
                binding = binding,
                navigateToPodcast = navigateToPodcast,
                showUnsubscribeDialog = showUnsubscribeDialog,
            )
        }
    }
}