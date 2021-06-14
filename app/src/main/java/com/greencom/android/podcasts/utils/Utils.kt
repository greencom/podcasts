package com.greencom.android.podcasts.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.greencom.android.podcasts.R
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/** Global tag for logging. */
const val GLOBAL_TAG = "global___"

/** Duration used to create crossfade animations. */
const val DURATION_CROSSFADE_ANIMATION = 150L

/**
 * Reveal or hide a view depending on the [show] parameter. This function also sets the
 * alpha of the view to zero when the view is being hidden.
 *
 * @param show show or hide a view.
 * @param animate perform with a crossfade animation or not. Defaults to `true`.
 * @param toAlpha can be used as a custom alpha value when showing a view. Not used when
 *                the view is being hidden from the screen. Defaults to `1F`.
 */
fun View.show(show: Boolean, animate: Boolean = true, toAlpha: Float = 1F) {
    when {
        show && animate -> revealCrossfade(toAlpha)
        show && !animate -> revealImmediately(toAlpha)
        !show && animate -> hideCrossfade()
        !show && !animate -> hideImmediately()
    }
}

/** Reveal a view immediately. */
fun View.revealImmediately(toAlpha: Float = 1F) {
    isVisible = true
    alpha = toAlpha
}

/** Hide a view immediately. This function also sets the alpha to zero. */
fun View.hideImmediately() {
    isVisible = false
    alpha = 0F
}

/** Reveal a view with crossfade animation. */
@Suppress("UsePropertyAccessSyntax")
fun View.revealCrossfade(toAlpha: Float = 1F) {
    if (isVisible && alpha == toAlpha) return
    isVisible = true
    animate()
        .alpha(toAlpha)
        .setDuration(DURATION_CROSSFADE_ANIMATION)
}

/** Hide a view with a crossfade animation. */
fun View.hideCrossfade() {
    if (isGone && alpha == 0F) return
    animate()
        .alpha(0F)
        .setDuration(DURATION_CROSSFADE_ANIMATION)
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

/** Converts the current position to a String timestamp that represents the current time. */
@ExperimentalTime
fun timestampCurrent(position: Long, context: Context): String {
    Duration.milliseconds(position).toComponents { hours, minutes, seconds, _ ->
        return when (hours) {
            0 -> context.getString(R.string.time_stamp_current_format_m_s, minutes, seconds)
            else -> context.getString(R.string.time_stamp_current_format_h_m_s, hours, minutes, seconds)
        }
    }
}

/** Converts the current position to a String timestamp that represents the remaining time. */
@ExperimentalTime
fun timestampLeft(position: Long, duration: Long, context: Context): String {
    Duration.milliseconds(duration - position).toComponents { hours, minutes, seconds, _ ->
        return when (hours) {
            0 -> context.getString(R.string.time_stamp_left_format_m_s, minutes, seconds)
            else -> context.getString(R.string.time_stamp_left_format_h_m_s, hours, minutes, seconds)
        }
    }
}

/**
 * App-specific default Coil builder. Uses [context] to access resource files.
 * Applies rounded corners transformation, crossfade animation and sets resources for the
 * placeholder and the error.
 */
fun ImageRequest.Builder.coilDefaultBuilder(context: Context) {
    transformations(RoundedCornersTransformation(
        context.resources.getDimension(R.dimen.coil_rounded_corners)
    ))
    crossfade(true)
    placeholder(R.drawable.shape_placeholder)
    error(R.drawable.shape_placeholder)
}