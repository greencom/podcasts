package com.greencom.android.podcasts.ui.podcast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.button.MaterialButton
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.ItemPodcastEpisodeBinding
import com.greencom.android.podcasts.databinding.ItemPodcastHeaderBinding
import com.greencom.android.podcasts.utils.PodcastWithEpisodesDiffCallback
import com.greencom.android.podcasts.utils.audioLengthToString
import com.greencom.android.podcasts.utils.pubDateToString
import com.greencom.android.podcasts.utils.setupSubscribeToggleButton
import kotlinx.coroutines.*

private const val ITEM_VIEW_TYPE_PODCAST_HEADER = 0
private const val ITEM_VIEW_TYPE_EPISODE = 1

/**
 * Adapter used for RecyclerView that represents a list consisting of a podcast header and
 * a list of podcast episodes.
 */
class PodcastWithEpisodesAdapter(
    private val updateSubscription: (String, Boolean) -> Unit,
) : ListAdapter<PodcastWithEpisodesDataItem, RecyclerView.ViewHolder>(
    PodcastWithEpisodesDiffCallback
) {

    /** Adapter coroutine scope. */
    private val adapterScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_PODCAST_HEADER -> PodcastHeaderViewHolder.create(
                parent,
                updateSubscription
            )
            ITEM_VIEW_TYPE_EPISODE -> EpisodeViewHolder.create(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PodcastHeaderViewHolder -> {
                val podcast = getItem(position) as PodcastWithEpisodesDataItem.PodcastHeader
                holder.bind(podcast.podcast)
            }
            is EpisodeViewHolder -> {
                val episode = getItem(position) as PodcastWithEpisodesDataItem.EpisodeItem
                holder.bind(episode.episode)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PodcastWithEpisodesDataItem.PodcastHeader -> ITEM_VIEW_TYPE_PODCAST_HEADER
            is PodcastWithEpisodesDataItem.EpisodeItem -> ITEM_VIEW_TYPE_EPISODE
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Cancel coroutine scope.
        adapterScope.cancel()
    }

    /**
     * Takes a podcast header and an episode list to calculate the list of the appropriate
     * items to be displayed in RecyclerView. Calculations are performed in the
     * background thread.
     */
    fun submitHeaderAndList(podcast: Podcast, episodes: List<Episode>) = adapterScope.launch {
        val items = listOf(PodcastWithEpisodesDataItem.PodcastHeader(podcast)) +
                episodes.map { PodcastWithEpisodesDataItem.EpisodeItem(it) }
        withContext(Dispatchers.Main) { submitList(items) }
    }
}

/** PodcastHeaderViewHolder represents a podcast header in the list. */
class PodcastHeaderViewHolder private constructor(
    private val binding: ItemPodcastHeaderBinding,
    private val updateSubscription: (String, Boolean) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    // View context.
    private val context = binding.root.context
    // Podcast associated with this ViewHolder.
    private lateinit var podcast: Podcast

    init {
        // Update subscription to the podcast.
        binding.subscribe.setOnClickListener {
            updateSubscription(podcast.id, (it as MaterialButton).isChecked)
            // Keep the button checked until the user makes his choice in the UnsubscribeDialog.
            if (podcast.subscribed) binding.subscribe.isChecked = true
        }

        // Expand and collapse description text.
        binding.description.setOnClickListener {
            binding.description.apply {
                maxLines = if (maxLines == 5) 50 else 5
            }
        }
    }

    /** Bind PodcastViewHolder with a given [Podcast]. */
    fun bind(podcast: Podcast) {
        // Update the podcast associated with this ViewHolder.
        this.podcast = podcast

        binding.cover.load(podcast.image) {
            transformations(RoundedCornersTransformation(
                context.resources.getDimension(R.dimen.corner_radius_medium)
            ))
            crossfade(true)
            placeholder(R.drawable.shape_placeholder)
            error(R.drawable.shape_placeholder)
        }
        binding.title.text = podcast.title
        binding.publisher.text = podcast.publisher
        binding.description.text = podcast.description
        binding.explicitContent.isVisible = podcast.explicitContent
        binding.episodeCount.text = context.resources.getQuantityString(
            R.plurals.podcast_episode_count,
            podcast.episodeCount,
            podcast.episodeCount
        )

        // "Subscribe" button setup.
        setupSubscribeToggleButton(binding.subscribe, podcast.subscribed, context)
    }

    companion object {
        /** Create a [PodcastHeaderViewHolder]. */
        fun create(
            parent: ViewGroup,
            updateSubscription: (String, Boolean) -> Unit,
        ): PodcastHeaderViewHolder {
            val binding = ItemPodcastHeaderBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return PodcastHeaderViewHolder(binding, updateSubscription)
        }
    }
}

/** EpisodeViewHolder represents a single episode item in the list. */
class EpisodeViewHolder private constructor(
    private val binding: ItemPodcastEpisodeBinding,
) : RecyclerView.ViewHolder(binding.root) {

    // View context.
    private val context = binding.root.context

    // Current time in millis.
    private val currentTime = System.currentTimeMillis()

    /** Bind EpisodeViewHolder with a given [Episode]. */
    fun bind(episode: Episode) {
        binding.title.text = episode.title
        // Date formatting.
        binding.date.text = pubDateToString(episode.date, currentTime, context)
        // Episode length formatting.
        binding.play.text = audioLengthToString(episode.audioLength, context)
    }

    companion object {
        /** Create an [EpisodeViewHolder]. */
        fun create(
            parent: ViewGroup,
        ): EpisodeViewHolder {
            val binding = ItemPodcastEpisodeBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return EpisodeViewHolder(binding)
        }
    }
}

/** Sealed class that abstracts items for the [PodcastWithEpisodesDataItem]. */
sealed class PodcastWithEpisodesDataItem {

    // ID used to calculate the difference between items in DiffUtil.
    abstract val id: String

    /** Represents a podcast header in the list. */
    data class PodcastHeader(val podcast: Podcast) : PodcastWithEpisodesDataItem() {
        override val id = podcast.id
    }

    /** Represents an episode item in the list. */
    data class EpisodeItem(val episode: Episode) : PodcastWithEpisodesDataItem() {
        override val id = episode.id
    }
}