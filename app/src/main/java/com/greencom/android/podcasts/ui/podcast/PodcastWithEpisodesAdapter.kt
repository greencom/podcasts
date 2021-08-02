package com.greencom.android.podcasts.ui.podcast

import android.animation.ObjectAnimator
import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
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
import kotlin.time.ExperimentalTime

private const val ITEM_VIEW_TYPE_PODCAST_HEADER = 0
private const val ITEM_VIEW_TYPE_EPISODE = 1

private const val DESCRIPTION_MIN_LINES = 5
private const val DESCRIPTION_MAX_LINES = 100

private const val SORT_ORDER_ANIMATION_DURATION = 200L

/**
 * Adapter used for RecyclerView that represents a list consisting of a podcast header and
 * a list of podcast episodes.
 */
class PodcastWithEpisodesAdapter(
    val sortOrder: StateFlow<SortOrder>,
    private val navigateToEpisode: (String) -> Unit,
    private val updateSubscription: (String, Boolean) -> Unit,
    private val changeSortOrder: () -> Unit,
    private val playEpisode: (String) -> Unit,
    private val play: () -> Unit,
    private val pause: () -> Unit,
) : ListAdapter<PodcastWithEpisodesDataItem, RecyclerView.ViewHolder>(
    PodcastWithEpisodesDiffCallback
) {

    /** Adapter's coroutine scope. Defaults to [Dispatchers.Default]. */
    private var scope: CoroutineScope? = null

    /** Whether the podcast description is expanded. False at start. */
    private var isPodcastDescriptionExpanded = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_PODCAST_HEADER -> PodcastHeaderViewHolder.create(
                parent = parent,
                updateSubscription = updateSubscription,
                changeSortOrder = changeSortOrder
            )
            ITEM_VIEW_TYPE_EPISODE -> PodcastEpisodeViewHolder.create(
                parent = parent,
                navigateToEpisode = navigateToEpisode,
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
            is PodcastEpisodeViewHolder -> {
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        // Init coroutine scope.
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Cancel coroutine scope.
        scope?.cancel()
    }

    /**
     * Takes a podcast header and an episode list to calculate the list of the appropriate
     * items to be displayed in RecyclerView. Calculations are performed in the
     * background thread.
     */
    fun submitHeaderAndList(podcast: Podcast, episodes: List<Episode>) = scope?.launch {
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

    private val context: Context
        get() = binding.root.context

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
            if (podcast.subscribed) {
                binding.subscribe.isChecked = true
            }
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

        binding.apply {
            cover.load(podcast.image) { coverBuilder(context) }
            title.text = podcast.title
            publisher.text = podcast.publisher
            explicitContent.isVisible = podcast.explicitContent
            episodeCount.text = context.resources.getQuantityString(
                R.plurals.podcast_episode_count,
                podcast.episodeCount,
                podcast.episodeCount
            )
            binding.sortOrder.rotation = if (sortOrder == SortOrder.RECENT_FIRST) 0F else 180F
            setupSubscribeToggleButton(subscribe, podcast.subscribed, context)

            // Handle podcast description.
            description.text = HtmlCompat.fromHtml(
                podcast.description,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            ).trim()
            description.movementMethod = LinkMovementMethod.getInstance()

            // Handle description state.
            val isDescriptionCollapsed = !isDescriptionExpanded && isDescriptionExpandable
            descriptionTrailingGradient.isVisible = isDescriptionCollapsed
            descriptionMore.isVisible = isDescriptionCollapsed
            descriptionArrowDown.isVisible = isDescriptionCollapsed
            description.maxLines = if (isDescriptionExpanded) DESCRIPTION_MAX_LINES else DESCRIPTION_MIN_LINES
        }
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
                duration = SORT_ORDER_ANIMATION_DURATION
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

/** PodcastEpisodeViewHolder represents a single episode item in the list. */
class PodcastEpisodeViewHolder private constructor(
    private val navigateToEpisode: (String) -> Unit,
    private val binding: ItemPodcastEpisodeBinding,
    private val playEpisode: (String) -> Unit,
    private val play: () -> Unit,
    private val pause: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val context: Context
        get() = binding.root.context

    /** Episode associated with this ViewHolder. */
    private lateinit var episode: Episode

    init {
        // Navigate to EpisodeFragment.
        binding.root.setOnClickListener {
            navigateToEpisode(episode.id)
        }

        // Resume or pause depending on the current state or play if the episode is not selected.
        binding.play.setOnClickListener {
            when {
                episode.isSelected && episode.isPlaying -> pause()
                episode.isSelected && !episode.isPlaying -> play()
                else -> playEpisode(episode.id)
            }
        }
    }

    /** Bind EpisodeViewHolder with a given [Episode]. */
    @ExperimentalTime
    fun bind(episode: Episode) {
        this.episode = episode
        binding.apply {
            title.text = episode.title
            date.text = episodePubDateToString(episode.date, context)
            setupPlayButton(play, episode, context)

            // Change title color depending on whether the episode is completed.
            if (episode.isCompleted) {
                val completedColor = TypedValue()
                context.theme.resolveAttribute(
                    R.attr.colorOnSurfaceLow,
                    completedColor,
                    true
                )
                title.setTextColor(completedColor.data)
            } else {
                val defaultColor = TypedValue()
                context.theme.resolveAttribute(
                    R.attr.colorOnSurfaceHigh,
                    defaultColor,
                    true
                )
                title.setTextColor(defaultColor.data)
            }
        }
    }

    companion object {
        /** Create an [PodcastEpisodeViewHolder]. */
        fun create(
            parent: ViewGroup,
            navigateToEpisode: (String) -> Unit,
            playEpisode: (String) -> Unit,
            play: () -> Unit,
            pause: () -> Unit,
        ): PodcastEpisodeViewHolder {
            val binding = ItemPodcastEpisodeBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return PodcastEpisodeViewHolder(
                binding = binding,
                navigateToEpisode = navigateToEpisode,
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