package com.greencom.android.podcasts.utils

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.utils.extensions.getColorStateListCompat
import com.greencom.android.podcasts.utils.extensions.getDrawableCompat
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/** Global tag for logging. */
const val GLOBAL_TAG = "global___"

/** Tag for player-specific logging. */
const val PLAYER_TAG = "PlayerTag"

/** Duration used to create crossfade animations. */
const val DURATION_CROSSFADE_ANIMATION = 150L

/** Duration used to delay text views marquee animation. */
const val DURATION_TEXT_MARQUEE_DELAY = 2000L

/** Start a vector drawable animation in the appropriate way depending on the system version. */
fun Drawable.animateVectorDrawable() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        (this as AnimatedVectorDrawable).start()
    } else {
        (this as AnimatedVectorDrawableCompat).start()
    }
}

/** Show a Snackbar with a given string res ID message. */
fun showSnackbar(view: View, @StringRes stringRes: Int) {
    Snackbar.make(view, stringRes, Snackbar.LENGTH_SHORT).show()
}

/** Allows control over whether the given AppBarLayout can be dragged or not. */
fun setAppBarLayoutCanDrag(appBarLayout: AppBarLayout, canDrag: Boolean) {
    if (appBarLayout.layoutParams != null) {
        val appBarParams = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        val appBarBehavior = AppBarLayout.Behavior()
        appBarBehavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return canDrag
            }
        })
        appBarParams.behavior = appBarBehavior
    }
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

/** Set up a given material button as a `Subscribe` toggle button. */
fun setupSubscribeToggleButton(button: MaterialButton, subscribed: Boolean, context: Context) {
    button.apply {
        if (subscribed) {
            isChecked = true
            text = context.getString(R.string.subscribed)
            icon = context.getDrawableCompat(R.drawable.ic_check_24)
        } else {
            isChecked = false
            text = context.getString(R.string.subscribe)
            icon = context.getDrawableCompat(R.drawable.ic_add_24)
        }
    }
}

/** Set up a given material button as `Play` button. */
@ExperimentalTime
fun setupPlayButton(button: MaterialButton, episode: Episode, context: Context) {
    button.apply {
        when {
            episode.isPlaying -> {
                text = context.getString(R.string.podcast_episode_playing)
                icon = context.getDrawableCompat(R.drawable.ic_animated_bar_chart_24)
                iconTint = context.getColorStateListCompat(R.color.primary_color)
                icon.animateVectorDrawable()
            }
            episode.isBuffering -> {
                text = context.getString(R.string.podcast_episode_buffering)
                icon = context.getDrawableCompat(R.drawable.ic_line_24)
                iconTint = context.getColorStateListCompat(R.color.primary_color)
            }
            episode.isCompleted -> {
                text = context.getString(R.string.podcast_episode_completed)
                icon = context.getDrawableCompat(R.drawable.ic_check_24)
                iconTint = context.getColorStateListCompat(R.color.green)
            }
            episode.isSelected -> {
                text = episodeTimeLeftToString(
                    position = episode.position,
                    duration = Duration.seconds(episode.audioLength),
                    context = context
                )
                icon = context.getDrawableCompat(R.drawable.ic_animated_bar_chart_24)
                iconTint = context.getColorStateListCompat(R.color.on_surface_hint_color)
            }
            episode.position > 0 -> {
                text = episodeTimeLeftToString(
                    position = episode.position,
                    duration = Duration.seconds(episode.audioLength),
                    context = context
                )
                icon = context.getDrawableCompat(
                    getIncompletePlayIconId(
                        position = episode.position,
                        duration = Duration.seconds(episode.audioLength).inWholeMilliseconds
                    )
                )
                // `ic_play_circle_outline_*_percent` drawables already contain the appropriate colors.
                iconTint = null
            }
            else -> {
                text = episodeDurationToString(Duration.seconds(episode.audioLength), context)
                icon = context.getDrawableCompat(R.drawable.ic_play_circle_outline_24)
                iconTint = context.getColorStateListCompat(R.color.primary_color)
            }
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
fun episodeDurationToString(duration: Duration, context: Context): String {
    duration.toComponents { hours, minutes, seconds, _ ->
        if (duration.inWholeSeconds <= 60) return context.getString(R.string.podcast_episode_duration_m, 1)

        var mMinutes = if (seconds >= 30) minutes + 1 else minutes
        val mHours = if (mMinutes == 60) {
            mMinutes = 0 // Reset minutes.
            hours + 1
        } else hours

        return when {
            mHours == 0 && mMinutes != 0 -> context.getString(R.string.podcast_episode_duration_m, mMinutes)
            mHours != 0 && mMinutes == 0 -> context.getString(R.string.podcast_episode_duration_h, mHours)
            else -> context.getString(R.string.podcast_episode_duration_h_m, mHours, mMinutes)
        }
    }
}

/**
 * Convert a date in milliseconds to a localized String. If an episode was published no more
 * than 7 days ago, return the most appropriate date description, otherwise return the date
 * in the format `day, month, year`.
 */
@ExperimentalTime
fun episodeDateToString(pubDate: Long, context: Context): String {
    val timeFromNow = System.currentTimeMillis() - pubDate
    Duration.milliseconds(timeFromNow).toComponents { days, hours, _, _, _ ->
        return when {
            days == 0 && hours < 1 -> context.getString(R.string.podcast_episode_pub_just_now)
            days == 0 && hours >= 1 -> context.resources.getQuantityString(R.plurals.podcast_episode_pub_hours_ago, hours, hours)
            days in 1..7 -> context.resources.getQuantityString(R.plurals.podcast_episode_pub_days_ago, days, days)
            else -> {
                val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
                dateFormatter.format(pubDate)
            }
        }
    }
}

/** Convert the episode's remaining time to a localized string. */
@ExperimentalTime
fun episodeTimeLeftToString(position: Long, duration: Duration, context: Context): String {
    val timeLeft = (duration - Duration.milliseconds(position))
    if (timeLeft.inWholeSeconds <= 60) return context.getString(R.string.podcast_episode_time_left_almost_over)

    timeLeft.toComponents { hours, minutes, seconds, _ ->
        var mMinutes = if (seconds >= 30) minutes + 1 else minutes
        val mHours = if (mMinutes == 60) {
            mMinutes = 0 // Reset minutes.
            hours + 1
        } else hours

        return when {
            mHours == 0 && mMinutes != 0 -> context.getString(R.string.podcast_episode_time_left_m, mMinutes)
            mHours != 0 && mMinutes == 0 -> context.getString(R.string.podcast_episode_time_left_h, mHours)
            else -> context.getString(R.string.podcast_episode_time_left_h_m, mHours, mMinutes)
        }
    }
}

/**
 * Default Coil builder for a 300x300px podcast's cover. Uses [context] to access resource files.
 * Applies rounded corners transformation, crossfade animation and sets resources for the
 * placeholder and the error.
 */
fun ImageRequest.Builder.coilCoverBuilder(context: Context) {
    transformations(RoundedCornersTransformation(
        context.resources.getDimension(R.dimen.coil_rounded_corners)
    ))
    crossfade(true)
    placeholder(R.drawable.shape_placeholder)
    error(R.drawable.shape_placeholder)
}

/**
 * Returns the appropriate `ic_play_circle_outline_*_percent` drawable ID depending
 * on the episode position.
 */
private fun getIncompletePlayIconId(position: Long, duration: Long): Int {
    val completionPercentage = (position.toFloat() / duration * 100).roundToInt()
    return when (completionPercentage) {
        in 0 until 20 -> R.drawable.ic_play_circle_outline_10_percent_24
        in 20 until 30 -> R.drawable.ic_play_circle_outline_20_percent_24
        in 30 until 40 -> R.drawable.ic_play_circle_outline_30_percent_24
        in 40 until 50 -> R.drawable.ic_play_circle_outline_40_percent_24
        in 50 until 60 -> R.drawable.ic_play_circle_outline_50_percent_24
        in 60 until 70 -> R.drawable.ic_play_circle_outline_60_percent_24
        in 70 until 80 -> R.drawable.ic_play_circle_outline_70_percent_24
        in 80 until 90 -> R.drawable.ic_play_circle_outline_80_percent_24
        in 90 until 100 -> R.drawable.ic_play_circle_outline_90_percent_24
        else -> throw IllegalArgumentException("Invalid completion percentage")
    }
}