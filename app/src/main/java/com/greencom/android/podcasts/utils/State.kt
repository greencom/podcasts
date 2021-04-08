package com.greencom.android.podcasts.utils

/**
 * Sealed class that represents the state of a process. The state could be either [Loading],
 * [Success] or [Error].
 */
sealed class State {
    object Loading : State()
    data class Success<T>(val data: T) : State()
    data class Error(val throwable: Throwable) : State()
}