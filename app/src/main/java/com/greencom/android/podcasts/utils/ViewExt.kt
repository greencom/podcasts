package com.greencom.android.podcasts.utils

import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible

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