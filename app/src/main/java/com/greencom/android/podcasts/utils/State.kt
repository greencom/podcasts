package com.greencom.android.podcasts.utils

/**
 * Sealed class that represents the state of the event and used as a wrapper
 * for a data (or error) if needed. Available states: [Loading], [Success] and
 * [Error]. [NotLoading] state is used as initial value for StateFlow.
 */
sealed class State {
    object NotLoading : State()
    object Loading : State()
    data class Success<out T>(val data: T) : State()
    data class Error(val throwable: Throwable) : State()
}

/**
 * Used as a wrapper for data. Contains event [Status] and [Throwable] (if needed).
 * Use factory methods [loading], [success], [error] to get an instance that matches
 * needs.
 */
data class StateExample<out T>(
    val status: Status,
    val data: T?,
    val throwable: Throwable?
) {
    companion object {
        /** Returns the [StateExample] instance represents the loading event. */
        fun <T> loading(): StateExample<T> = StateExample(Status.LOADING, null, null)
        /** Returns the [StateExample] instance represents the success event. */
        fun <T> success(data: T?): StateExample<T> = StateExample(Status.SUCCESS, data, null)
        /** Returns the [StateExample] instance represents the event with throwable. */
        fun <T> error(throwable: Throwable): StateExample<T> = StateExample(Status.ERROR, null, throwable)
    }
}

/** Enum class that describes the status of the event. */
enum class Status {
    LOADING,
    SUCCESS,
    ERROR
}