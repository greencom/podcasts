package com.greencom.android.podcasts.player

/** Object that contains custom MediaSession's commands. */
object CustomSessionCommand {

    /** Action for 'SET_SLEEP_TIMER' custom command. */
    const val SET_SLEEP_TIMER = "CUSTOM_COMMAND_SET_SLEEP_TIMER"

    /**
     * Key used to pass a Long duration value in ms along with the
     * [SET_SLEEP_TIMER] custom command.
     */
    const val SET_SLEEP_TIMER_DURATION_KEY = "SLEEP_TIMER_DURATION_KEY"

    /** Action for 'REMOVE_SLEEP_TIMER' custom command. */
    const val REMOVE_SLEEP_TIMER = "CUSTOM_COMMAND_REMOVE_SLEEP_TIMER"
}