package com.greencom.android.podcasts.utils

import com.google.android.material.appbar.AppBarLayout
import com.greencom.android.podcasts.utils.AppBarLayoutStateChangeListener.AppBarLayoutState
import com.greencom.android.podcasts.utils.AppBarLayoutStateChangeListener.AppBarLayoutState.*
import kotlin.math.abs

/**
 * Abstract class definition for a callback to be invoked when an [AppBarLayout]'s state
 * changes. State is represented by [AppBarLayoutState].
 */
abstract class AppBarLayoutStateChangeListener : AppBarLayout.OnOffsetChangedListener {

    /**
     * Enum class that defines [AppBarLayout]'s states. The state could be either [EXPANDED]
     * or [COLLAPSED] or [IDLE].
     */
    enum class AppBarLayoutState {
        EXPANDED,
        COLLAPSED,
        IDLE
    }

    private var currentState = IDLE

    /** Called when the AppBarLayout's state has been changed. */
    abstract fun onStateChanged(appBarLayout: AppBarLayout, newState: AppBarLayoutState)

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
                if (currentState != IDLE) {
                    onStateChanged(appBarLayout, IDLE)
                }
                currentState = IDLE
            }
        }
    }
}