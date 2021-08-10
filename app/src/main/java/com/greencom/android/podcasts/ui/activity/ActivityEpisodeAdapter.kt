package com.greencom.android.podcasts.ui.activity

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.databinding.ItemActivityEpisodeBinding
import com.greencom.android.podcasts.utils.*
import kotlin.time.ExperimentalTime

/**
 * RecyclerView adapter used by
 * [ActivityBookmarksFragment][com.greencom.android.podcasts.ui.activity.bookmarks.ActivityBookmarksFragment] and
 * [ActivityInProgressFragment][com.greencom.android.podcasts.ui.activity.inprogress.ActivityInProgressFragment].
 */
class ActivityEpisodeAdapter(
    private val navigateToEpisode: (String) -> Unit,
    private val onInBookmarksChange: (String, Boolean) -> Unit,
    private val playEpisode: (String) -> Unit,
    private val play: () -> Unit,
    private val pause: () -> Unit,
    private val showEpisodeOptions: (String, Boolean) -> Unit,
) : ListAdapter<Episode, ActivityEpisodeAdapter.ViewHolder>(EpisodeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(
            parent = parent,
            navigateToEpisode = navigateToEpisode,
            onInBookmarksChange = onInBookmarksChange,
            playEpisode = playEpisode,
            play = play,
            pause = pause,
            showEpisodeOptions = showEpisodeOptions
        )
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episode = getItem(position)
        holder.bind(episode)
    }

    /** ViewHolder that represents a single episode in the bookmarks list. */
    class ViewHolder private constructor(
        private val binding: ItemActivityEpisodeBinding,
        private val navigateToEpisode: (String) -> Unit,
        private val onInBookmarksChange: (String, Boolean) -> Unit,
        private val playEpisode: (String) -> Unit,
        private val play: () -> Unit,
        private val pause: () -> Unit,
        private val showEpisodeOptions: (String, Boolean) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context: Context
            get() = binding.root.context

        /** Episode associated with this ViewHolder. */
        private lateinit var episode: Episode

        init {
            // Navigate to an episode page.
            binding.root.setOnClickListener {
                navigateToEpisode(episode.id)
            }

            // Show an EpisodeOptionsDialog.
            binding.root.setOnLongClickListener {
                showEpisodeOptions(episode.id, episode.isCompleted)
                true
            }

            // Resume or pause depending on the current state or play if the episode is not selected.
            binding.play.setOnClickListener {
                when {
                    episode.isSelected && episode.isPlaying -> pause()
                    episode.isSelected && !episode.isPlaying -> play()
                    else -> playEpisode(episode.id)
                }
            }

            // Add the episode to the bookmarks or remove from there.
            binding.inBookmarks.setOnClickListener {
                onInBookmarksChange(episode.id, !episode.inBookmarks)
            }
        }

        /** Bind ViewHolder with a given [Episode]. */
        @ExperimentalTime
        fun bind(episode: Episode) {
            this.episode = episode
            binding.apply {
                cover.load(episode.image) { coilCoverBuilder(context) }
                dateAndPodcastTitle.text = buildString {
                    append(episodeDateToString(episode.date, context))
                    append(" ${Symbol.bullet} ")
                    append(episode.podcastTitle)
                }
                title.text = episode.title
                setupPlayButton(play, episode, context)

                // Set up "Add to bookmarks" button.
                if (episode.inBookmarks) {
                    inBookmarks.setImageResource(R.drawable.ic_playlist_check_24)
                    inBookmarks.imageTintList = context.getColorStateList(R.color.green)
                    inBookmarks.contentDescription =
                        context.getString(R.string.episode_remove_from_bookmarks_description)
                } else {
                    inBookmarks.setImageResource(R.drawable.ic_playlist_add_24)
                    inBookmarks.imageTintList = context.getColorStateList(R.color.primary_color)
                    inBookmarks.contentDescription =
                        context.getString(R.string.episode_add_to_bookmarks_description)
                }
            }
        }

        companion object {
            /** Create a [ViewHolder]. */
            fun create(
                parent: ViewGroup,
                navigateToEpisode: (String) -> Unit,
                onInBookmarksChange: (String, Boolean) -> Unit,
                playEpisode: (String) -> Unit,
                play: () -> Unit,
                pause: () -> Unit,
                showEpisodeOptions: (String, Boolean) -> Unit,
            ): ViewHolder {
                val binding = ItemActivityEpisodeBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(
                    binding = binding,
                    navigateToEpisode = navigateToEpisode,
                    onInBookmarksChange = onInBookmarksChange,
                    playEpisode = playEpisode,
                    play = play,
                    pause = pause,
                    showEpisodeOptions = showEpisodeOptions,
                )
            }
        }
    }
}