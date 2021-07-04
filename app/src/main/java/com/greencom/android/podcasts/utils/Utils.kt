package com.greencom.android.podcasts.utils

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.greencom.android.podcasts.R
import java.text.SimpleDateFormat
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

/**
 * Reveal or hide a view depending on the [show] parameter. This function also sets the
 * alpha of the view to zero when the view is being hidden.
 *
 * @param show show or hide a view.
 * @param animate perform with a crossfade animation or not. Defaults to `true`.
 * @param toAlpha can be used as a custom alpha value when showing a view. Not used when
 *                the view is being hidden from the screen. Defaults to `1F`.
 * @param duration animation duration in millis.
 */
fun View.show(
    show: Boolean,
    animate: Boolean = true,
    toAlpha: Float = 1F,
    duration: Long = DURATION_CROSSFADE_ANIMATION,
) {
    when {
        show && animate -> revealCrossfade(toAlpha, duration)
        show && !animate -> revealImmediately(toAlpha)
        !show && animate -> hideCrossfade(duration)
        !show && !animate -> hideImmediately()
    }
}

/** Reveal a view immediately. */
fun View.revealImmediately(toAlpha: Float = 1F) {
    alpha = toAlpha
    isVisible = true
}

/** Hide a view immediately. This function also sets the alpha to zero. */
fun View.hideImmediately() {
    alpha = 0F
    isVisible = false
}

/** Reveal a view with crossfade animation. */
@Suppress("UsePropertyAccessSyntax")
fun View.revealCrossfade(toAlpha: Float = 1F, duration: Long = DURATION_CROSSFADE_ANIMATION) {
    if (isVisible && alpha == toAlpha) return
    isVisible = true
    animate()
        .alpha(toAlpha)
        .setDuration(duration)
}

/** Hide a view with a crossfade animation. */
fun View.hideCrossfade(duration: Long = DURATION_CROSSFADE_ANIMATION) {
    if (isGone && alpha == 0F) return
    animate()
        .alpha(0F)
        .setDuration(duration)
        .withEndAction { isVisible = false }
}

/**
 * Hide a view immediately. This function also sets the alpha to zero.
 * Note: this function uses [View.animate] with duration set to zero.
 */
fun View.hideImmediatelyWithAnimation() {
    animate()
        .alpha(0F)
        .setDuration(0L)
        .withEndAction { isVisible = false }
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
fun initSwipeToRefresh(swipeToRefresh: SwipeRefreshLayout, context: Context) {
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
fun episodePubDateToString(pubDate: Long, context: Context): String {
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
fun ImageRequest.Builder.coverBuilder(context: Context) {
    transformations(RoundedCornersTransformation(
        context.resources.getDimension(R.dimen.coil_rounded_corners)
    ))
    crossfade(true)
    placeholder(R.drawable.shape_placeholder)
    error(R.drawable.shape_placeholder)
}