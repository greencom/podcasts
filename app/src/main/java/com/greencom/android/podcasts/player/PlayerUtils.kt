package com.greencom.android.podcasts.player

import androidx.media2.player.MediaPlayer

// TODO

const val PLAYER_SKIP_FORWARD_VALUE = 30_000
const val PLAYER_SKIP_BACKWARD_VALUE = -10_000

fun Int.isPlayerPlaying(): Boolean = this == MediaPlayer.PLAYER_STATE_PLAYING

fun Int.isPlayerPaused(): Boolean = this == MediaPlayer.PLAYER_STATE_PAUSED

fun Int.isPlayerIdle(): Boolean = this == MediaPlayer.PLAYER_STATE_IDLE

fun Int.isPlayerError(): Boolean = this == MediaPlayer.PLAYER_STATE_ERROR