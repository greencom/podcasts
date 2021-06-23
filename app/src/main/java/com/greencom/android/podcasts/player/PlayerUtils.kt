package com.greencom.android.podcasts.player

import androidx.media2.player.MediaPlayer

// TODO
fun Int.isPlayerPlaying(): Boolean = this == MediaPlayer.PLAYER_STATE_PLAYING

// TODO
fun Int.isPlayerNotPlaying(): Boolean = this != MediaPlayer.PLAYER_STATE_PLAYING

// TODO
fun Int.isPlayerPaused(): Boolean = this == MediaPlayer.PLAYER_STATE_PAUSED

// TODO
fun Int.isPlayerIdle(): Boolean = this == MediaPlayer.PLAYER_STATE_IDLE

// TODO
fun Int.isPlayerError(): Boolean = this == MediaPlayer.PLAYER_STATE_ERROR