package com.greencom.android.podcasts.utils

import androidx.recyclerview.widget.DiffUtil
import com.greencom.android.podcasts.data.domain.Podcast

/** Callback for calculating the diff between two non-null [Podcast]s in a list. */
object PodcastDiffCallback : DiffUtil.ItemCallback<Podcast>() {
    override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem == newItem
    }
}