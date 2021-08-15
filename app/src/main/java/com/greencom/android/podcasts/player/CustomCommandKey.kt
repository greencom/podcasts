package com.greencom.android.podcasts.player

/**
 * Object that stores keys to pass the appropriate data along with custom commands
 * defined in the [CustomCommand] object.
 */
object CustomCommandKey {

    /**
     * Key used to pass a String episode ID along with the
     * [CustomCommand.SET_EPISODE_AND_PLAY_FROM_TIMECODE] custom command.
     */
    const val SET_EPISODE_AND_PLAY_FROM_TIMECODE_EPISODE_ID_KEY =
        "CUSTOM_COMMAND_SET_EPISODE_AND_PLAY_FROM_TIMECODE_EPISODE_ID_KEY"

    /**
     * Key used to pass a Long timecode value in ms along with the
     * [CustomCommand.SET_EPISODE_AND_PLAY_FROM_TIMECODE] custom command.
     */
    const val SET_EPISODE_AND_PLAY_FROM_TIMECODE_TIMECODE_KEY =
        "CUSTOM_COMMAND_SET_EPISODE_AND_PLAY_FROM_TIMECODE_TIMECODE_KEY"

    /**
     * Key used to pass a Long duration value in ms along with the
     * [CustomCommand.SET_SLEEP_TIMER] custom command.
     */
    const val SET_SLEEP_TIMER_DURATION_KEY = "CUSTOM_COMMAND_SET_SLEEP_TIMER_DURATION_KEY"
}