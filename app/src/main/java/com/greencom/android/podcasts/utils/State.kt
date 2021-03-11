package com.greencom.android.podcasts.utils

/**
 * Sealed class that represents the state of the event and used as a wrapper
 * for a data (or error) if needed. Available states: [Loading], [Success] and
 * [Error]. [Init] state is used as initial value for StateFlow.
 */
sealed class State {
    object Init : State()
    object Loading : State()
    data class Success<out T>(val data: T) : State()
    data class Error(val throwable: Throwable) : State()
}