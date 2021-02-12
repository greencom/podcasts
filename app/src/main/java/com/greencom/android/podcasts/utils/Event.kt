package com.greencom.android.podcasts.utils

/**
 * Used as a wrapper for data. Contains event [Status] and [Throwable] (if needed).
 * Use factory methods [loading], [success], [error] to get an instance that matches
 * needs.
 */
data class Event<out T>(
    val status: Status,
    val data: T?,
    val throwable: Throwable?
) {
    companion object {
        /** Returns the [Event] instance represents the loading event. */
        fun <T> loading(): Event<T> = Event(Status.LOADING, null, null)
        /** Returns the [Event] instance represents the success event. */
        fun <T> success(data: T?): Event<T> = Event(Status.SUCCESS, data, null)
        /** Returns the [Event] instance represents the event with throwable. */
        fun <T> error(throwable: Throwable): Event<T> = Event(Status.ERROR, null, throwable)
    }
}

/** Enum class that describes the status of the event. */
enum class Status {
    LOADING,
    SUCCESS,
    ERROR
}
