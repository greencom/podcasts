package com.greencom.android.podcasts.player

import androidx.media2.common.MediaMetadata
import androidx.media2.player.MediaPlayer

/** Media metadata string key for episode ID. */
const val EPISODE_ID = MediaMetadata.METADATA_KEY_MEDIA_ID

/** Media metadata string key for episode title. */
const val EPISODE_TITLE = MediaMetadata.METADATA_KEY_TITLE

/** Media metadata string key for episode publisher. */
const val EPISODE_PUBLISHER = MediaMetadata.METADATA_KEY_AUTHOR

/** Media metadata string key for episode image URI. */
const val EPISODE_IMAGE = MediaMetadata.METADATA_KEY_ART_URI

/** Media metadata string key for episode duration. */
const val EPISODE_DURATION = MediaMetadata.METADATA_KEY_DURATION

/** Media metadata string key for episode start position. */
const val EPISODE_START_POSITION = "EPISODE_START_POSITION"

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