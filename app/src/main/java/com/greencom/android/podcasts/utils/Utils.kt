package com.greencom.android.podcasts.utils

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.ui.podcast.PodcastWithEpisodesDataItem
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Global tag for logging. */
const val GLOBAL_TAG = "global___"

/** String message to use as an exception message to show the appropriate Snackbar. */
const val NO_CONNECTION = "NO_CONNECTION"

/** Duration used to create crossfade animations. */
const val ANIMATION_DURATION_CROSSFADE = 150L

/** Duration used to animate player slider thumb radius. */
const val ANIMATION_DURATION_SLIDER_THUMB = 120L

/**
 * Enum class that represents a sort order. Enum [value]s can be used in ListenApiService
 * methods.
 */
enum class SortOrder(val value: String) {
    /** Contains "recent_first" value. */
    RECENT_FIRST("recent_first"),

    /** Contains "oldest_first" value. */
    OLDEST_FIRST("oldest_first");

    /** Reverse [SortOrder] value. */
    fun reverse(): SortOrder = if (this == RECENT_FIRST) OLDEST_FIRST else RECENT_FIRST
}

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

/** Reveal a view with crossfade animation. */
fun View.revealCrossfade() {
    isVisible = true
    animate()
        .alpha(1f)
        .setDuration(ANIMATION_DURATION_CROSSFADE)
}

/** Hide a view with a crossfade animation. */
fun View.hideCrossfade() {
    animate()
        .alpha(0f)
        .setDuration(ANIMATION_DURATION_CROSSFADE)
        .withEndAction { isVisible = false }
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

/**
 * Convert a length in seconds to a localized String. Three outputs are possible
 * (examples for EN locale):
 * - "1 hr", if the number of hours is not zero and the number of minutes is zero.
 * - "35 min", if the number of hours is zero.
 * - "1 hr 35 min" in any other case.
 */
fun audioLengthToString(length: Int, context: Context): String {
    val hours = (length / TimeUnit.HOURS.toSeconds(1)).toInt()
    val minutes = ((length - TimeUnit.HOURS.toSeconds(1) * hours) /
            TimeUnit.MINUTES.toSeconds(1).toFloat()).roundToInt()

    return when {
        hours != 0 && minutes == 0 -> context.getString(R.string.podcast_length_hours, hours)
        hours == 0 -> context.getString(R.string.podcast_length_minutes, minutes)
        else -> context.getString(R.string.podcast_length_full, hours, minutes)
    }
}

/**
 * Convert a date in milliseconds to a localized String. If an episode was published no more
 * than 7 days ago, return the most appropriate date description, otherwise return the date
 * in the format `day, month, year`.
 */
fun pubDateToString(pubDate: Long, currentDate: Long, context: Context): String {
    return when (val timeFromNow = currentDate - pubDate) {
        in (0..TimeUnit.HOURS.toMillis(1)) -> context.getString(R.string.podcast_just_now)
        in (TimeUnit.HOURS.toMillis(1)..TimeUnit.DAYS.toMillis(1)) -> {
            val hours = timeFromNow / TimeUnit.HOURS.toMillis(1)
            context.resources.getQuantityString(R.plurals.podcast_hours_ago, hours.toInt(), hours)
        }
        in (TimeUnit.DAYS.toMillis(1))..TimeUnit.DAYS.toMillis(7) -> {
            val days = timeFromNow / TimeUnit.DAYS.toMillis(1)
            context.resources.getQuantityString(R.plurals.podcast_days_ago, days.toInt(), days)
        }
        else -> {
            val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
            dateFormatter.format(pubDate)
        }
    }
}