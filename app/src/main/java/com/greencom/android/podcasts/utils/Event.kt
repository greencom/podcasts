package com.greencom.android.podcasts.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents a one-time event.
 */
class Event<out T>(private val data: T) {

    /** Whether the event has been handled. */
    @Suppress("MemberVisibilityCanBePrivate")
    var hasBeenHandled = false
        private set // Allow external read but not write

    /** Returns the content and prevents its use again. */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            data
        }
    }

    /** Returns the content, even if it has already been handled. */
    @Suppress("UNUSED")
    fun peekContent(): T = data
}

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the
 * [Event]'s data has already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents
 * has not been handled.
 *
 * Alternative for the [observeEvent] method.
 *
 * Usage:
 * `liveData.observe(lifecycleOwner, EventObserver {  })`
 */
class EventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        event?.getContentIfNotHandled()?.let(onEventUnhandledContent)
    }
}

/**
 * Used as replacement for the default [LiveData.observe] method to observing
 * [Event]'s. Simplifying the pattern of checking if the [Event]'s data
 * has already been handled.
 *
 * Alternative for the [EventObserver] class.
 *
 * Usage:
 * `liveData.observeEvent(lifecycleOwner) {  }`
 */
inline fun <T> LiveData<Event<T>>.observeEvent(
        owner: LifecycleOwner,
        crossinline onEventUnhandledContent: (T) -> Unit
) {
    observe(owner) { it?.getContentIfNotHandled()?.let(onEventUnhandledContent) }
}