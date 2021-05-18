package com.greencom.android.podcasts.ui.podcast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.databinding.EpisodeItemBinding
import com.greencom.android.podcasts.utils.EpisodeDiffCallback
import com.greencom.android.podcasts.utils.audioLengthToString
import com.greencom.android.podcasts.utils.pubDateToString

/** Adapter used for RecyclerView that represents a list of the podcast episodes. */
class PodcastEpisodeAdapter :
    ListAdapter<Episode, PodcastEpisodeViewHolder>(EpisodeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastEpisodeViewHolder {
        return PodcastEpisodeViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: PodcastEpisodeViewHolder, position: Int) {
        val episode = getItem(position)
        holder.bind(episode)
    }
}

/** ViewHolder that represents a single item in the episode list. */
class PodcastEpisodeViewHolder private constructor(
    private val binding: EpisodeItemBinding,
) : RecyclerView.ViewHolder(binding.root) {

    // View context.
    private val context = binding.root.context

    // Current time in millis.
    private val currentTime = System.currentTimeMillis()

    /** Bind ViewHolder with a given [Episode]. */
    fun bind(episode: Episode) {
        binding.title.text = episode.title
        // Date formatting.
        binding.date.text = pubDateToString(episode.date, currentTime, context)
        // Length formatting.
        binding.play.text = audioLengthToString(episode.audioLength, context)
    }

    companion object {
        /** Create a [PodcastEpisodeViewHolder]. */
        fun create(
            parent: ViewGroup,
        ): PodcastEpisodeViewHolder {
            val binding = EpisodeItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return PodcastEpisodeViewHolder(binding)
        }
    }
}