package com.greencom.android.podcasts.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents a one-time event.
 */
class SingleEvent<out T>(private val content: T) {

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
            content
        }
    }

    /** Returns the content, even if it has already been handled. */
    fun peekContent(): T = content
}

/**
 * An [Observer] for [SingleEvent]s, simplifying the pattern of checking if the
 * [SingleEvent]'s content has already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [SingleEvent]'s contents
 * has not been handled.
 *
 * Alternative for the [observeSingleEvent] method.
 *
 * Usage:
 * `liveData.observe(lifecycleOwner, SingleEventObserver {  })`
 */
class SingleEventObserver<T>(private val onEventUnhandledContent: (T) -> Unit) : Observer<SingleEvent<T>> {
    override fun onChanged(singleEvent: SingleEvent<T>?) {
        singleEvent?.getContentIfNotHandled()?.let(onEventUnhandledContent)
    }
}

/**
 * Used as replacement for the default [LiveData.observe] method to observing
 * [SingleEvent]'s. Simplifying the pattern of checking if the [SingleEvent]'s content
 * has already been handled.
 *
 * Alternative for the [SingleEventObserver] class.
 *
 * Usage:
 * `liveData.observeSingleEvent(lifecycleOwner) {  }`
 */
inline fun <T> LiveData<SingleEvent<T>>.observeSingleEvent(
        owner: LifecycleOwner,
        crossinline onEventUnhandledContent: (T) -> Unit
) {
    observe(owner) { it?.getContentIfNotHandled()?.let(onEventUnhandledContent) }
}
