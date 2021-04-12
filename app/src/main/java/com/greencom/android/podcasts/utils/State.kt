package com.greencom.android.podcasts.utils

/**
 * Sealed class that represents the state of a process. The state could be either [Loading],
 * [Success] or [Error].
 */
sealed class State {
    /** Object that represents the `Loading` state. */
    object Loading : State()
    /** Class that represents the `Success` state with [data]. */
    data class Success<out T>(val data: T) : State()
    /** Class that represents the `Error` state with [exception]. */
    data class Error(val exception: Throwable) : State()
}