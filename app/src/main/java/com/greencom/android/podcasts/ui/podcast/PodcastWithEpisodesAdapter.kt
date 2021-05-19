package com.greencom.android.podcasts.ui.podcast

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.databinding.PodcastEpisodeItemBinding
import com.greencom.android.podcasts.utils.EpisodeDiffCallback
import com.greencom.android.podcasts.utils.audioLengthToString
import com.greencom.android.podcasts.utils.pubDateToString

/** Adapter used for RecyclerView that represents a list of the podcast episodes. */
class PodcastWithEpisodesAdapter :
    ListAdapter<Episode, EpisodeViewHolder>(EpisodeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = getItem(position)
        holder.bind(episode)
    }
}

/** ViewHolder that represents a single item in the episode list. */
class EpisodeViewHolder private constructor(
    private val binding: PodcastEpisodeItemBinding,
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
        /** Create a [EpisodeViewHolder]. */
        fun create(
            parent: ViewGroup,
        ): EpisodeViewHolder {
            val binding = PodcastEpisodeItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return EpisodeViewHolder(binding)
        }
    }
}