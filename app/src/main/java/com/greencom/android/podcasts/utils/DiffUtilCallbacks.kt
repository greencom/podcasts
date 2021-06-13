package com.greencom.android.podcasts.utils

import androidx.recyclerview.widget.DiffUtil
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.ui.podcast.PodcastWithEpisodesDataItem

/** Callback for calculating the diff between two non-null [PodcastShort] items in a list. */
object PodcastDiffCallback : DiffUtil.ItemCallback<PodcastShort>() {
    override fun areItemsTheSame(oldItem: PodcastShort, newItem: PodcastShort): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PodcastShort, newItem: PodcastShort): Boolean {
        return oldItem == newItem
    }
}

/**
 * Callback for calculating the diff between two non-null [PodcastWithEpisodesDataItem]
 * items in a list.
 */
object PodcastWithEpisodesDiffCallback : DiffUtil.ItemCallback<PodcastWithEpisodesDataItem>() {
    override fun areItemsTheSame(
        oldItem: PodcastWithEpisodesDataItem,
        newItem: PodcastWithEpisodesDataItem
    ): Boolean {
        return when {
            oldItem is PodcastWithEpisodesDataItem.PodcastHeader &&
                    newItem is PodcastWithEpisodesDataItem.PodcastHeader -> oldItem.id == newItem.id

            oldItem is PodcastWithEpisodesDataItem.EpisodeItem &&
                    newItem is PodcastWithEpisodesDataItem.EpisodeItem -> oldItem.id == newItem.id

            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: PodcastWithEpisodesDataItem,
        newItem: PodcastWithEpisodesDataItem
    ): Boolean {
        return when {
            oldItem is PodcastWithEpisodesDataItem.PodcastHeader &&
                    newItem is PodcastWithEpisodesDataItem.PodcastHeader -> oldItem == newItem

            oldItem is PodcastWithEpisodesDataItem.EpisodeItem &&
                    newItem is PodcastWithEpisodesDataItem.EpisodeItem -> oldItem == newItem

            else -> false
        }
    }
}