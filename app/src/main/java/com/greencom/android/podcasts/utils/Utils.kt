package com.greencom.android.podcasts.utils

import androidx.recyclerview.widget.DiffUtil
import com.greencom.android.podcasts.data.domain.PodcastShort

/** Callback for calculating the diff between two non-null [PodcastShort] items in a list. */
object PodcastDiffCallback : DiffUtil.ItemCallback<PodcastShort>() {
    override fun areItemsTheSame(oldItem: PodcastShort, newItem: PodcastShort): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: PodcastShort, newItem: PodcastShort): Boolean {
        return oldItem == newItem
    }
}

/** Reveal animation duration. */
const val REVEAL_ANIMATION_DURATION = 150L