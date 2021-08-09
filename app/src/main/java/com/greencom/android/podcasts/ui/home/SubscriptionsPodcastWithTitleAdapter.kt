package com.greencom.android.podcasts.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.databinding.ItemSubscriptionsPodcastWithTitleBinding
import com.greencom.android.podcasts.utils.PodcastShortDiffCallback
import com.greencom.android.podcasts.utils.coilCoverBuilder

/**
 * Adapter used for RecyclerView that represents a grid list of subscriptions in the
 * [SUBSCRIPTION_MODE_GRID_WITH_TITLE] mode.
 */
class SubscriptionsPodcastWithTitleAdapter(
    private val navigateToPodcast: (String) -> Unit,
    private val showUnsubscribeDialog: (String) -> Unit,
) : ListAdapter<PodcastShort, SubscriptionsPodcastWithTitleViewHolder>(PodcastShortDiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubscriptionsPodcastWithTitleViewHolder {
        return SubscriptionsPodcastWithTitleViewHolder.create(
            parent = parent,
            navigateToPodcast = navigateToPodcast,
            showUnsubscribeDialog = showUnsubscribeDialog
        )
    }

    override fun onBindViewHolder(holder: SubscriptionsPodcastWithTitleViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast)
    }
}

/** ViewHolder that represents a single item in the list. */
class SubscriptionsPodcastWithTitleViewHolder private constructor(
    private val binding: ItemSubscriptionsPodcastWithTitleBinding,
    private val navigateToPodcast: (String) -> Unit,
    private val showUnsubscribeDialog: (String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val context: Context
        get() = binding.root.context

    /** Podcast associated with this ViewHolder. */
    private lateinit var podcast: PodcastShort

    init {
        // Navigate to a podcast page.
        binding.root.setOnClickListener {
            navigateToPodcast(podcast.id)
        }

        // Show an UnsubscribeDialog.
        binding.root.setOnLongClickListener {
            showUnsubscribeDialog(podcast.id)
            true
        }
    }

    /** Bind ViewHolder with a given [PodcastShort]. */
    fun bind(podcast: PodcastShort) {
        this.podcast = podcast

        binding.apply {
            cover.load(podcast.image) { coilCoverBuilder(context) }
            title.text = podcast.title
        }
    }

    companion object {
        /** Create a [SubscriptionsPodcastWithTitleViewHolder]. */
        fun create(
            parent: ViewGroup,
            navigateToPodcast: (String) -> Unit,
            showUnsubscribeDialog: (String) -> Unit,
        ): SubscriptionsPodcastWithTitleViewHolder {
            val binding = ItemSubscriptionsPodcastWithTitleBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return SubscriptionsPodcastWithTitleViewHolder(
                binding = binding,
                navigateToPodcast = navigateToPodcast,
                showUnsubscribeDialog = showUnsubscribeDialog,
            )
        }
    }
}