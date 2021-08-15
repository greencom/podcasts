package com.greencom.android.podcasts.player

/**
 * Object that defines custom MediaSession commands. See [CustomCommandKey] object
 * that stores the keys for passing the appropriate data along with these custom commands.
 */
object CustomCommand {

    /**
     * Custom command used to set an episode and play it from the timecode. See the
     * appropriate keys for passing ID and timecode in the [CustomCommandKey].
     */
    const val SET_EPISODE_AND_PLAY_FROM_TIMECODE = "CUSTOM_COMMAND_SET_EPISODE_AND_PLAY_FROM_TIMECODE"

    /**
     * Custom command used to set a sleep timer. See the appropriate key for passing duration
     * in the [CustomCommandKey].
     */
    const val SET_SLEEP_TIMER = "CUSTOM_COMMAND_SET_SLEEP_TIMER"

    /** Custom command used to remove a sleep timer. */
    const val REMOVE_SLEEP_TIMER = "CUSTOM_COMMAND_REMOVE_SLEEP_TIMER"

    /** Custom command used to mark the current episode as completed. */
    const val MARK_CURRENT_EPISODE_COMPLETED = "MARK_CURRENT_EPISODE_COMPLETED"
}