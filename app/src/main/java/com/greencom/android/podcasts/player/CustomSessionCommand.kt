package com.greencom.android.podcasts.player

/** Object that contains custom MediaSession's commands. */
object CustomSessionCommand {

    /**
     * Action for 'SET_EPISODE_AND_PLAY_FROM_TIMECODE' custom command. Use
     * [SET_EPISODE_AND_PLAY_FROM_TIMECODE_TIMECODE_KEY] and
     * [SET_EPISODE_AND_PLAY_FROM_TIMECODE_EPISODE_ID_KEY] keys to pass an episode ID and
     * timecode along with this custom command.
     */
    const val SET_EPISODE_AND_PLAY_FROM_TIMECODE = "CUSTOM_COMMAND_SET_EPISODE_AND_PLAY_FROM_TIMECODE"

    /**
     * Key used to pass a String episode ID along with the
     * [SET_EPISODE_AND_PLAY_FROM_TIMECODE] custom command.
     */
    const val SET_EPISODE_AND_PLAY_FROM_TIMECODE_EPISODE_ID_KEY =
        "CUSTOM_COMMAND_SET_EPISODE_AND_PLAY_FROM_TIMECODE_EPISODE_ID_KEY"

    /**
     * Key used to pass a Long timecode value in ms along with the
     * [SET_EPISODE_AND_PLAY_FROM_TIMECODE] custom command.
     */
    const val SET_EPISODE_AND_PLAY_FROM_TIMECODE_TIMECODE_KEY =
        "CUSTOM_COMMAND_SET_EPISODE_AND_PLAY_FROM_TIMECODE_TIMECODE_KEY"

    /**
     * Action for 'SET_SLEEP_TIMER' custom command. Use [SET_SLEEP_TIMER_DURATION_KEY] key
     * to pass a duration along with this custom command.
     */
    const val SET_SLEEP_TIMER = "CUSTOM_COMMAND_SET_SLEEP_TIMER"

    /**
     * Key used to pass a Long duration value in ms along with the
     * [SET_SLEEP_TIMER] custom command.
     */
    const val SET_SLEEP_TIMER_DURATION_KEY = "CUSTOM_COMMAND_SET_SLEEP_TIMER_DURATION_KEY"

    /** Action for 'REMOVE_SLEEP_TIMER' custom command. */
    const val REMOVE_SLEEP_TIMER = "CUSTOM_COMMAND_REMOVE_SLEEP_TIMER"

    /** Action for 'MARK_CURRENT_EPISODE_COMPLETED' custom command. */
    const val MARK_CURRENT_EPISODE_COMPLETED = "MARK_CURRENT_EPISODE_COMPLETED"
}