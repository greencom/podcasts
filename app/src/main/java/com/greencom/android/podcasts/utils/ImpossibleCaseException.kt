package com.greencom.android.podcasts.utils

/** An exception indicates that an impossible case has occurred in the 'when' expression. */
class ImpossibleCaseException(override val message: String) : Exception() {
    constructor(): this("Impossible 'when' case")
}