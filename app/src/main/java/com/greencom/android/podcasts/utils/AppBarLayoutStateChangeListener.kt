package com.greencom.android.podcasts.utils

import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

/**
 * Abstract class definition for a callback to be invoked when an [AppBarLayout]'s state
 * changes. The states are listed in the [AppBarLayoutStateChangeListener.State] object.
 */
abstract class AppBarLayoutStateChangeListener : AppBarLayout.OnOffsetChangedListener {

    /** State can [IDLE], [COLLAPSED], [EXPANDED] or [SETTLING]. */
    companion object State {

        /** AppBarLayout is in the initial state. */
        const val IDLE = 0

        /** AppBarLayout is in the collapsed state. */
        const val COLLAPSED = 1

        /** AppBarLayout is in the expanded state. */
        const val EXPANDED = 2

        /**
         * AppBarLayout is in the settling state. Settling means the AppBarLayout is moving to
         * either [COLLAPSED] or [EXPANDED] state.
         */
        const val SETTLING = 3
    }

    private var currentState = IDLE

    /**
     * Called when the AppBarLayout's state has been changed. The states are listed in the
     * [AppBarLayoutStateChangeListener.State] object.
     */
    abstract fun onStateChanged(appBarLayout: AppBarLayout, newState: Int)

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        when (abs(verticalOffset)) {
            0 -> {
                if (currentState != EXPANDED) {
                    onStateChanged(appBarLayout, EXPANDED)
                }
                currentState = EXPANDED
            }

            appBarLayout.totalScrollRange -> {
                if (currentState != COLLAPSED) {
                    onStateChanged(appBarLayout, COLLAPSED)
                }
                currentState = COLLAPSED
            }

            else -> {
                if (currentState != SETTLING) {
                    onStateChanged(appBarLayout, SETTLING)
                }
                currentState = SETTLING
            }
        }
    }
}