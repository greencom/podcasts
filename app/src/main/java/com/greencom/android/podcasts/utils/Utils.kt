package com.greencom.android.podcasts.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.database.PodcastEntity
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

/**
 * Filter the [PodcastEntity] list to exclude items that not represented in a given list.
 * This method uses [PodcastEntity.id]s to calculate the difference between lists.
 *
 * [PodcastEntity]s in the resulting list have `genreId` properties with [Podcast.NOT_IN_BEST]
 * value.
 *
 * @return Filtered `List<PodcastEntity>`.
 */
fun List<PodcastEntity>.filterNotIn(new: List<PodcastEntity>): List<PodcastEntity> {
    val newIds = new.map { it.id }
    return this
        .filter { it.id !in newIds }
        .map { it.copy(genreId = Podcast.NOT_IN_BEST) }
}

/**
 * Create a toast with configured gravity to show up above the player. Use this single toast
 * instead of creating multiple instances.
 */
@Suppress("UNUSED")
fun createToast(context: Context): Toast {
    return Toast.makeText(context, "", Toast.LENGTH_SHORT).apply {
        setGravity(
            Gravity.BOTTOM,
            0,
            context.resources.getDimensionPixelOffset(R.dimen.toast_y_offset)
        )
    }
}

/** Convert `dp` to `px`. */
@Suppress("UNUSED")
fun Context.dpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}

/** Convert `px` to `dp`. */
@Suppress("UNUSED")
fun Context.pxToDp(px: Float): Float {
    return px / resources.displayMetrics.density
}