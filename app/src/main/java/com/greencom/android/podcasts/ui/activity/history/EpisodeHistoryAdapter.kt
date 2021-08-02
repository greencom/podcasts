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
import com.greencom.android.podcasts.utils.coverBuilder
import com.greencom.android.podcasts.utils.episodePubDateToString
import kotlin.time.ExperimentalTime

/** Adapter used for RecyclerView that represents a history of completed episodes. */
class EpisodeHistoryAdapter(
    private val navigateToEpisode: (String) -> Unit,
) : ListAdapter<Episode, HistoryEpisodeViewHolder>(EpisodeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryEpisodeViewHolder {
        return HistoryEpisodeViewHolder.create(
            parent = parent,
            navigateToEpisode = navigateToEpisode,
        )
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: HistoryEpisodeViewHolder, position: Int) {
        val episode = getItem(position)
        holder.bind(episode)
    }
}

/** ViewHolder that represents a single item in the history of completed episodes. */
class HistoryEpisodeViewHolder private constructor(
    private val binding: ItemHistoryEpisodeBinding,
    private val navigateToEpisode: (String) -> Unit,
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
    }

    /** Bind ViewHolder with a given [Episode]. */
    @ExperimentalTime
    fun bind(episode: Episode) {
        this.episode = episode
        binding.apply {
            cover.load(episode.image) { coverBuilder(context) }
            date.text = buildString {
                append(context.getString(R.string.activity_history_completed))
                append(" ")
                val stamp = episodePubDateToString(episode.completionDate, context)
                append(stamp.lowercase())
            }
            title.text = episode.title
        }
    }

    companion object {
        /** Create a [HistoryEpisodeViewHolder]. */
        fun create(
            parent: ViewGroup,
            navigateToEpisode: (String) -> Unit,
        ): HistoryEpisodeViewHolder {
            val binding = ItemHistoryEpisodeBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return HistoryEpisodeViewHolder(
                binding = binding,
                navigateToEpisode = navigateToEpisode,
            )
        }
    }
}