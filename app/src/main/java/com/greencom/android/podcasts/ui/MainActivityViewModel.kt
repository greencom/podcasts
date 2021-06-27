package com.greencom.android.podcasts.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.media2.session.SessionToken
import com.greencom.android.podcasts.player.CurrentEpisode
import com.greencom.android.podcasts.player.PlayerServiceConnection
import com.greencom.android.podcasts.utils.PLAYER_TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.time.ExperimentalTime

// TODO
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection,
) : ViewModel() {

    private val _isPlayerBottomSheetExpanded = MutableStateFlow(false)
    val isPlayerBottomSheetExpanded = _isPlayerBottomSheetExpanded.asStateFlow()

    private val _skipBackwardOrForwardValue = MutableStateFlow(0L)
    val skipBackwardOrForwardValue = _skipBackwardOrForwardValue.asStateFlow()

    val currentEpisode: StateFlow<CurrentEpisode>
        get() = playerServiceConnection.currentEpisode

    val currentState: StateFlow<Int>
        get() = playerServiceConnection.currentState

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

    fun setPlayerBottomSheetState(isExpanded: Boolean) {
        _isPlayerBottomSheetExpanded.value = isExpanded
    }

    fun resetPlayerBottomSheetState() {
        _isPlayerBottomSheetExpanded.value = !_isPlayerBottomSheetExpanded.value
        _isPlayerBottomSheetExpanded.value = !_isPlayerBottomSheetExpanded.value
    }

    fun updateSkipBackwardOrForwardValue(value: Long) {
        _skipBackwardOrForwardValue.value += value
    }

    fun resetSkipBackwardOrForwardValue() {
        _skipBackwardOrForwardValue.value = 0L
    }

    @ExperimentalTime
    fun initPlayerServiceConnection(context: Context, sessionToken: SessionToken) {
        playerServiceConnection.initConnection(context, sessionToken)
    }

    fun closePlayerServiceConnection() {
        playerServiceConnection.closeConnection()
    }

    override fun onCleared() {
        Log.d(PLAYER_TAG, "MainActivityViewModel.onCleared()")
        super.onCleared()
        closePlayerServiceConnection()
    }
}