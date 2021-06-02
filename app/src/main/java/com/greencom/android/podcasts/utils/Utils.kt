package com.greencom.android.podcasts.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.ui.podcast.PodcastWithEpisodesDataItem
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/** Global tag for logging. */
const val GLOBAL_TAG = "global___"

/** Duration used to create crossfade animations. */
const val DURATION_CROSSFADE_ANIMATION = 150L

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

/** Show a Snackbar with a given string res ID message. */
fun showSnackbar(view: View, @StringRes stringRes: Int) {
    Snackbar.make(view, stringRes, Snackbar.LENGTH_SHORT).show()
}

/** Reveal a view immediately. */
fun View.revealImmediately() {
    isVisible = true
    animate()
        .alpha(1F)
        .setDuration(0L)
}

/** Reveal a view with crossfade animation. */
fun View.revealCrossfade() {
    isVisible = true
    animate()
        .alpha(1F)
        .setDuration(DURATION_CROSSFADE_ANIMATION)
}

/** Hide a view with a crossfade animation. */
fun View.hideCrossfade() {
    animate()
        .alpha(0F)
        .setDuration(DURATION_CROSSFADE_ANIMATION)
        .withEndAction { isVisible = false }
}

/** Swipe-to-refresh setup. */
fun setupSwipeToRefresh(swipeToRefresh: SwipeRefreshLayout, context: Context) {
    swipeToRefresh.apply {
        val color = TypedValue()
        val backgroundColor = TypedValue()
        context.theme?.resolveAttribute(R.attr.colorPrimary, color, true)
        context.theme?.resolveAttribute(
            R.attr.colorSwipeToRefreshBackground, backgroundColor, true
        )
        setColorSchemeColors(color.data)
        setProgressBackgroundColorSchemeColor(backgroundColor.data)
    }
}

/** Setup given material button as a `Subscribe` toggle button. */
fun setupSubscribeToggleButton(button: MaterialButton, subscribed: Boolean, context: Context) {
    button.apply {
        if (subscribed) {
            isChecked = true
            text = context.getString(R.string.subscribed)
            icon = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_check_24,
                context.theme
            )
        } else {
            isChecked = false
            text = context.getString(R.string.subscribe)
            icon = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_add_24,
                context.theme
            )
        }
    }
}

/**
 * Convert a length in seconds to a localized String. Three outputs are possible
 * (examples for EN locale):
 * - "1 hr", if the number of hours is not zero and the number of minutes is zero.
 * - "35 min", if the number of hours is zero.
 * - "1 hr 35 min" in any other case.
 */
@ExperimentalTime
fun audioLengthToString(length: Duration, context: Context): String {
    if (length.inWholeSeconds <= 60) return context.getString(R.string.episode_length_minutes, 1)

    val hours = length.inWholeHours.toInt()
    val minutes = ((length - Duration.hours(hours)) / Duration.minutes(1)).roundToInt()

    return when {
        hours != 0 && minutes != 0 -> context.getString(R.string.episode_length_full, hours, minutes)
        hours != 0 && minutes == 0 -> context.getString(R.string.episode_length_hours, hours)
        else -> context.getString(R.string.episode_length_minutes, minutes)
    }
}

/**
 * Convert a date in milliseconds to a localized String. If an episode was published no more
 * than 7 days ago, return the most appropriate date description, otherwise return the date
 * in the format `day, month, year`.
 */
fun pubDateToString(pubDate: Long, context: Context): String {
    return when (val timeFromNow = System.currentTimeMillis() - pubDate) {

        // Just now.
        in (0..TimeUnit.HOURS.toMillis(1)) -> context.getString(R.string.episode_pub_just_now)

        // N hours ago.
        in (TimeUnit.HOURS.toMillis(1)..TimeUnit.DAYS.toMillis(1)) -> {
            val hours = timeFromNow / TimeUnit.HOURS.toMillis(1)
            context.resources.getQuantityString(R.plurals.episode_pub_hours_ago, hours.toInt(), hours)
        }

        // N days ago.
        in (TimeUnit.DAYS.toMillis(1))..TimeUnit.DAYS.toMillis(7) -> {
            val days = timeFromNow / TimeUnit.DAYS.toMillis(1)
            context.resources.getQuantityString(R.plurals.episode_pub_days_ago, days.toInt(), days)
        }

        // Date.
        else -> {
            val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
            dateFormatter.format(pubDate)
        }
    }
}