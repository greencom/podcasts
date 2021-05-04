package com.greencom.android.podcasts.utils

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.greencom.android.podcasts.R
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

/** Show a Snackbar with a given string res ID message. */
fun showSnackbar(view: View, @StringRes stringRes: Int) {
    Snackbar.make(view, stringRes, Snackbar.LENGTH_SHORT).show()
}

/** Reveal animation duration. */
const val REVEAL_ANIMATION_DURATION = 150L

/** Reveal a view with crossfade animation. */
fun View.reveal() {
    animate()
        .alpha(1f)
        .setDuration(REVEAL_ANIMATION_DURATION)
}

/** Setup given material button as a `Subscribe` toggle button. */
fun setupSubscribeToggleButton(button: MaterialButton, subscribed: Boolean, context: Context) {
    button.apply {
        if (subscribed) {
            isChecked = true
            text = context.getString(R.string.explore_subscribed)
            icon = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_check_24,
                context.theme
            )
        } else {
            isChecked = false
            text = context.getString(R.string.explore_subscribe)
            icon = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_add_24,
                context.theme
            )
        }
    }
}