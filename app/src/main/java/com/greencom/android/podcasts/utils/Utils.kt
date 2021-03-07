package com.greencom.android.podcasts.utils

import android.content.Context

/** Convert `dp` to `px`. */
fun Context.dpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}

/** Convert `px` to `dp`. */
fun Context.pxToDp(px: Float): Float {
    return px / resources.displayMetrics.density
}