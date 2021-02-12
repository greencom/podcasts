package com.greencom.android.podcasts.utils

/**
 * Used as a wrapper for data. Contains event [Status] and [Throwable] (if needed).
 * Use factory methods [loading], [success], [error] to get an instance that matches
 * needs.
 */
// TODO: Rename
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
