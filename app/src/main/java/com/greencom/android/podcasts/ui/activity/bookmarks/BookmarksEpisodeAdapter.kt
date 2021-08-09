package com.greencom.android.podcasts.ui.activity.bookmarks

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.databinding.ItemBookmarksEpisodeBinding
import com.greencom.android.podcasts.utils.*
import kotlin.time.ExperimentalTime

/** Adapter used for RecyclerView that represents a list of episodes added to bookmarks. */
class BookmarksEpisodeAdapter(
    private val navigateToEpisode: (String) -> Unit,
    private val removeFromBookmarks: (String) -> Unit,
    private val playEpisode: (String) -> Unit,
    private val play: () -> Unit,
    private val pause: () -> Unit,
    private val onLongClick: (String, Boolean) -> Unit,
) : ListAdapter<Episode, BookmarksEpisodeAdapter.ViewHolder>(EpisodeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(
            parent = parent,
            navigateToEpisode = navigateToEpisode,
            removeFromBookmarks = removeFromBookmarks,
            playEpisode = playEpisode,
            play = play,
            pause = pause,
            showEpisodeOptions = onLongClick
        )
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episode = getItem(position)
        holder.bind(episode)
    }

    /** ViewHolder that represents a single episode in the bookmarks list. */
    class ViewHolder private constructor(
        private val binding: ItemBookmarksEpisodeBinding,
        private val navigateToEpisode: (String) -> Unit,
        private val removeFromBookmarks: (String) -> Unit,
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

            // Remove from bookmarks.
            binding.removeFromBookmarks.setOnClickListener {
                removeFromBookmarks(episode.id)
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
            }
        }

        companion object {
            /** Create a [ViewHolder]. */
            fun create(
                parent: ViewGroup,
                navigateToEpisode: (String) -> Unit,
                removeFromBookmarks: (String) -> Unit,
                playEpisode: (String) -> Unit,
                play: () -> Unit,
                pause: () -> Unit,
                showEpisodeOptions: (String, Boolean) -> Unit,
            ): ViewHolder {
                val binding = ItemBookmarksEpisodeBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(
                    binding = binding,
                    navigateToEpisode = navigateToEpisode,
                    removeFromBookmarks = removeFromBookmarks,
                    playEpisode = playEpisode,
                    play = play,
                    pause = pause,
                    showEpisodeOptions = showEpisodeOptions,
                )
            }
        }
    }
}