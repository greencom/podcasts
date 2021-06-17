package com.greencom.android.podcasts.ui.podcast

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.ItemPodcastEpisodeBinding
import com.greencom.android.podcasts.databinding.ItemPodcastHeaderBinding
import com.greencom.android.podcasts.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private const val ITEM_VIEW_TYPE_PODCAST_HEADER = 0
private const val ITEM_VIEW_TYPE_EPISODE = 1

private const val DESCRIPTION_MIN_LINES = 5
private const val DESCRIPTION_MAX_LINES = 100

private const val DURATION_SORT_ORDER_ANIMATION = 200L

/**
 * Adapter used for RecyclerView that represents a list consisting of a podcast header and
 * a list of podcast episodes.
 */
class PodcastWithEpisodesAdapter(
    val sortOrder: StateFlow<SortOrder>,
    private val updateSubscription: (String, Boolean) -> Unit,
    private val changeSortOrder: () -> Unit,
    private val playEpisode: (Episode) -> Unit,
    private val play: () -> Unit,
    private val pause: () -> Unit,
) : ListAdapter<PodcastWithEpisodesDataItem, RecyclerView.ViewHolder>(
    PodcastWithEpisodesDiffCallback
) {

    private val adapterJob = SupervisorJob()
    private val adapterScope = CoroutineScope(adapterJob + Dispatchers.Default)

    /** Whether the podcast description is expanded. False at start. */
    private var isPodcastDescriptionExpanded = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_PODCAST_HEADER -> PodcastHeaderViewHolder.create(
                parent = parent,
                updateSubscription = updateSubscription,
                changeSortOrder = changeSortOrder
            )
            ITEM_VIEW_TYPE_EPISODE -> EpisodeViewHolder.create(
                parent = parent,
                playEpisode = playEpisode,
                play = play,
                pause = pause
            )
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PodcastHeaderViewHolder -> {
                val podcast = getItem(position) as PodcastWithEpisodesDataItem.PodcastHeader
                holder.bind(podcast.podcast, sortOrder.value, isPodcastDescriptionExpanded)
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

    /** Expand or collapse the podcast description depending on its current state. */
    fun expandOrCollapseDescription() {
        // Update description state.
        isPodcastDescriptionExpanded = isPodcastDescriptionExpanded.not()
        // Make the adapter redraw the podcast header with a new description state.
        notifyItemChanged(0)
    }
}

/** PodcastHeaderViewHolder represents a podcast header in the list. */
class PodcastHeaderViewHolder private constructor(
    private val binding: ItemPodcastHeaderBinding,
    private val updateSubscription: (String, Boolean) -> Unit,
    private val changeSortOrder: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val context = binding.root.context

    /** Podcast associated with this ViewHolder. */
    private lateinit var podcast: Podcast

    /** Whether the description is expandable depending on the line count. */
    private var isDescriptionExpandable = true

    private var sortOrderAnimator: ObjectAnimator? = null

    init {
        // Update subscription to the podcast.
        binding.subscribe.setOnClickListener {
            updateSubscription(podcast.id, (it as MaterialButton).isChecked)
            // Keep the button checked until the user makes his choice in the UnsubscribeDialog.
            if (podcast.subscribed) binding.subscribe.isChecked = true
        }

        // Check line count when the description is laid out.
        binding.description.doOnLayout {
            if (binding.description.lineCount <= DESCRIPTION_MIN_LINES) {
                // Do not show 'More' button and do not set a click listener.
                isDescriptionExpandable = false
                binding.descriptionTrailingGradient.isVisible = false
                binding.descriptionMore.isVisible = false
                binding.descriptionArrowDown.isVisible = false
            } else {
                // Set OnClickListener to expand and collapse description text.
                binding.description.setOnClickListener {
                    (bindingAdapter as PodcastWithEpisodesAdapter).expandOrCollapseDescription()
                }
            }
        }

        // Change sort order.
        binding.sortOrder.setOnClickListener {
            changeSortOrder()
            when ((bindingAdapter as PodcastWithEpisodesAdapter).sortOrder.value) {
                SortOrder.RECENT_FIRST -> rotateSortOrder(0F)
                SortOrder.OLDEST_FIRST -> rotateSortOrder(180F)
            }
        }
    }

    /** Bind PodcastViewHolder with a given [Podcast]. */
    fun bind(podcast: Podcast, sortOrder: SortOrder, isDescriptionExpanded: Boolean) {
        this.podcast = podcast

        binding.cover.load(podcast.image) { coverBuilder(context) }
        binding.title.text = podcast.title
        binding.publisher.text = podcast.publisher
        binding.description.text = HtmlCompat.fromHtml(
            podcast.description,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()
        binding.explicitContent.isVisible = podcast.explicitContent
        binding.episodeCount.text = context.resources.getQuantityString(
            R.plurals.podcast_episode_count,
            podcast.episodeCount,
            podcast.episodeCount
        )
        binding.sortOrder.rotation = if (sortOrder == SortOrder.RECENT_FIRST) 0F else 180F

        // "Subscribe" button setup.
        setupSubscribeToggleButton(binding.subscribe, podcast.subscribed, context)

        // Handle description state.
        binding.descriptionTrailingGradient.isVisible = !isDescriptionExpanded && isDescriptionExpandable
        binding.descriptionMore.isVisible = !isDescriptionExpanded && isDescriptionExpandable
        binding.descriptionArrowDown.isVisible = !isDescriptionExpanded && isDescriptionExpandable
        binding.description.maxLines =
            if (isDescriptionExpanded) DESCRIPTION_MAX_LINES else DESCRIPTION_MIN_LINES
    }

    /** Rotate the 'Sort order' button to a given value. */
    private fun rotateSortOrder(to: Float) {
        if (sortOrderAnimator != null) {
            sortOrderAnimator?.setFloatValues(to)
        } else {
            sortOrderAnimator = ObjectAnimator.ofFloat(
                binding.sortOrder,
                "rotation",
                to
            ).apply {
                duration = DURATION_SORT_ORDER_ANIMATION
                setAutoCancel(true)
            }
        }
        sortOrderAnimator?.start()
    }

    companion object {
        /** Create a [PodcastHeaderViewHolder]. */
        fun create(
            parent: ViewGroup,
            updateSubscription: (String, Boolean) -> Unit,
            changeSortOrder: () -> Unit,
        ): PodcastHeaderViewHolder {
            val binding = ItemPodcastHeaderBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return PodcastHeaderViewHolder(
                binding = binding,
                updateSubscription = updateSubscription,
                changeSortOrder = changeSortOrder,
            )
        }
    }
}

/** EpisodeViewHolder represents a single episode item in the list. */
class EpisodeViewHolder private constructor(
    private val binding: ItemPodcastEpisodeBinding,
    private val playEpisode: (Episode) -> Unit,
    private val play: () -> Unit,
    private val pause: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val context = binding.root.context

    /** Episode associated with this ViewHolder. */
    private lateinit var episode: Episode

    init {
        // TODO
        binding.play.setOnClickListener {
            if (episode.isSelected) {
                if (episode.isPlaying) pause() else play()
            } else {
                playEpisode(episode)
            }
        }
    }

    /** Bind EpisodeViewHolder with a given [Episode]. */
    @ExperimentalTime
    fun bind(episode: Episode) {
        this.episode = episode

        binding.title.text = episode.title
        binding.date.text = episodePubDateToString(episode.date, context)

        // TODO: Add check for a current position and for isCompleted.
        binding.play.apply {
            when {
                episode.isPlaying -> {
                    text = context.getString(R.string.podcast_playing)
                    icon = AppCompatResources.getDrawable(context, R.drawable.ic_animated_bar_chart_24)
                    icon.animateVectorDrawable()
                }
                episode.isSelected -> {
                    text = episodeTimeLeftToString(episode.position, Duration.seconds(episode.audioLength), context)
                    icon = AppCompatResources.getDrawable(context, R.drawable.ic_play_circle_outline_24)
                }
                else -> {
                    text = episodeDurationToString(Duration.seconds(episode.audioLength), context)
                    icon = AppCompatResources.getDrawable(context, R.drawable.ic_play_circle_outline_24)
                }
            }
        }
    }

    companion object {
        /** Create an [EpisodeViewHolder]. */
        fun create(
            parent: ViewGroup,
            playEpisode: (Episode) -> Unit,
            play: () -> Unit,
            pause: () -> Unit,
        ): EpisodeViewHolder {
            val binding = ItemPodcastEpisodeBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return EpisodeViewHolder(
                binding = binding,
                playEpisode = playEpisode,
                play = play,
                pause = pause,
            )
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