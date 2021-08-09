package com.greencom.android.podcasts.ui.activity.history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.databinding.ItemHistoryEpisodeBinding
import com.greencom.android.podcasts.utils.EpisodeDiffCallback
import com.greencom.android.podcasts.utils.Symbol
import com.greencom.android.podcasts.utils.coilCoverBuilder
import com.greencom.android.podcasts.utils.episodeDateToString
import kotlin.time.ExperimentalTime

/** Adapter used for RecyclerView that represents a history of completed episodes. */
class HistoryEpisodeAdapter(
    private val navigateToEpisode: (String) -> Unit,
    private val onLongClick: (String, Boolean) -> Unit,
) : ListAdapter<Episode, HistoryEpisodeAdapter.ViewHolder>(EpisodeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(
            parent = parent,
            navigateToEpisode = navigateToEpisode,
            showEpisodeOptions = onLongClick
        )
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episode = getItem(position)
        holder.bind(episode)
    }

    /** ViewHolder that represents a single item in the history of completed episodes. */
    class ViewHolder private constructor(
        private val binding: ItemHistoryEpisodeBinding,
        private val navigateToEpisode: (String) -> Unit,
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
        }

        /** Bind ViewHolder with a given [Episode]. */
        @ExperimentalTime
        fun bind(episode: Episode) {
            this.episode = episode
            binding.apply {
                cover.load(episode.image) { coilCoverBuilder(context) }
                dateAndPodcastTitle.text = buildString {
                    append(context.getString(R.string.activity_history_completed))
                    append(" ")
                    append(episodeDateToString(episode.completionDate, context).lowercase())
                    append(" ${Symbol.bullet} ")
                    append(episode.podcastTitle)
                }
                title.text = episode.title
            }
        }

        companion object {
            /** Create a [ViewHolder]. */
            fun create(
                parent: ViewGroup,
                navigateToEpisode: (String) -> Unit,
                showEpisodeOptions: (String, Boolean) -> Unit,
            ): ViewHolder {
                val binding = ItemHistoryEpisodeBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(
                    binding = binding,
                    navigateToEpisode = navigateToEpisode,
                    showEpisodeOptions = showEpisodeOptions,
                )
            }
        }
    }
}