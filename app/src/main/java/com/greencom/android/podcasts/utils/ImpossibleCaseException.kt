package com.greencom.android.podcasts.utils

/** An exception indicates that an impossible case has occurred in the 'when' expression. */
class ImpossibleCaseException(private val additionalMessage: String) : Exception() {

    override val message: String
        get() = if (additionalMessage.isBlank()) {
            "Impossible 'when' case"
        } else {
            "Impossible 'when' case: $additionalMessage"
        }

    constructor(): this("")
}