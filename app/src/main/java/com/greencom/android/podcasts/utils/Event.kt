package com.greencom.android.podcasts.utils

/**
 * Used as a wrapper for data. Contains event [Status] and [Error] (if needed).
 * Use factory methods [success], [loading], [error] to get an instance that matches
 * needs.
 */
data class Event<out T>(
    val status: Status,
    val data: T?,
    val error: Error?
) {
    companion object {
        /** Returns the [Event] instance represents the loading event. */
        fun <T> loading(): Event<T> = Event(Status.LOADING, null, null)
        /** Returns the [Event] instance represents the success event. */
        fun <T> success(data: T?): Event<T> = Event(Status.SUCCESS, data, null)
        /** Returns the [Event] instance represents the event with error. */
        fun <T> error(error: Error): Event<T> = Event(Status.ERROR, null, error)
    }
}

/** Enum class that describes the status of the event. */
enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}
