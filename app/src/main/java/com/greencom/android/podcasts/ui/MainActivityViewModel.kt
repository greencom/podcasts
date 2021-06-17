package com.greencom.android.podcasts.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media2.session.SessionToken
import com.greencom.android.podcasts.player.PlayerServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// TODO
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection,
) : ViewModel() {

    val currentEpisode: StateFlow<PlayerServiceConnection.CurrentEpisode>
        get() = playerServiceConnection.currentEpisode

    val playerState: StateFlow<Int>
        get() = playerServiceConnection.playerState

    val currentPosition: StateFlow<Long>
        get() = playerServiceConnection.currentPosition

    val isPlaying: Boolean
        get() = playerServiceConnection.isPlaying

    val isPaused: Boolean
        get() = playerServiceConnection.isPaused

    val duration: Long
        get() = playerServiceConnection.duration

    fun play() {
        playerServiceConnection.play()
    }

    fun pause() {
        playerServiceConnection.pause()
    }

    fun seekTo(position: Long) {
        playerServiceConnection.seekTo(position)
    }

    fun initPlayerServiceConnection(context: Context, sessionToken: SessionToken) {
        playerServiceConnection.initConnection(context, sessionToken)
    }

    override fun onCleared() {
        super.onCleared()
        playerServiceConnection.close()
    }
}