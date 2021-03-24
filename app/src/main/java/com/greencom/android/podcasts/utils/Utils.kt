package com.greencom.android.podcasts.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast

/** Global tag for logging. */
const val GLOBAL_TAG = "GLOBAL_TAG"

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
 * Create a toast with configured gravity to show up above the player. Use this single toast
 * instead of creating multiple instances.
 */
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
fun Context.dpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}

/** Convert `px` to `dp`. */
fun Context.pxToDp(px: Float): Float {
    return px / resources.displayMetrics.density
}