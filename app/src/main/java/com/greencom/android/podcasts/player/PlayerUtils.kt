package com.greencom.android.podcasts.player

import androidx.media2.common.MediaMetadata
import androidx.media2.player.MediaPlayer

/**
 * Value used to skip backward for.
 *
 * Note: sign respects skip direction.
 */
const val PLAYER_SKIP_BACKWARD_VALUE = -10_000L

/**
 * Value used to skip forward for.
 *
 * Note: sign respects skip direction.
 */
const val PLAYER_SKIP_FORWARD_VALUE = 30_000L

/** Returns `true` if the player state is [MediaPlayer.PLAYER_STATE_PLAYING]. `false` otherwise. */
fun Int.isPlayerPlaying(): Boolean = this == MediaPlayer.PLAYER_STATE_PLAYING

/** Returns `true` if the player state is [MediaPlayer.PLAYER_STATE_PAUSED]. `false` otherwise. */
fun Int.isPlayerPaused(): Boolean = this == MediaPlayer.PLAYER_STATE_PAUSED

/** Returns `true` if the player state is [MediaPlayer.PLAYER_STATE_IDLE]. `false` otherwise. */
fun Int.isPlayerIdle(): Boolean = this == MediaPlayer.PLAYER_STATE_IDLE

/** Returns `true` if the player state is [MediaPlayer.PLAYER_STATE_ERROR]. `false` otherwise. */
fun Int.isPlayerError(): Boolean = this == MediaPlayer.PLAYER_STATE_ERROR

/** Object that contains episode metadata keys. */
object EpisodeMetadata {

    /** Media metadata string key for episode ID. */
    const val ID = MediaMetadata.METADATA_KEY_MEDIA_ID

    /** Media metadata string key for episode title. */
    const val TITLE = MediaMetadata.METADATA_KEY_TITLE

    /** Media metadata string key for episode publisher. */
    const val PUBLISHER = MediaMetadata.METADATA_KEY_AUTHOR

    /** Media metadata string key for episode image URI. */
    const val IMAGE = MediaMetadata.METADATA_KEY_ART_URI

    /** Media metadata string key for episode duration. */
    const val DURATION = MediaMetadata.METADATA_KEY_DURATION
}

// TODO
object CustomSessionCommand {

    // TODO
    const val RESET_PLAYER = "CUSTOM_COMMAND_RESET_PLAYER"
}