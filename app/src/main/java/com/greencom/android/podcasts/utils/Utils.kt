package com.greencom.android.podcasts.utils

import android.content.Context

/**
 * Convert `dp` to `px`.
 *
 * @param dp a value in `Float`.
 *
 * @return A float value to represent `px` equivalent to `dp` depending on device density.
 */
fun Context.convertDpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}

/**
 * Convert `px` to `dp`.
 *
 * @param px a value in `Float`.
 *
 * @return A float value to represent `dp` equivalent to `px` depending on device density.
 */
fun Context.convertPxToDp(px: Float): Float {
    return px / resources.displayMetrics.density
}
