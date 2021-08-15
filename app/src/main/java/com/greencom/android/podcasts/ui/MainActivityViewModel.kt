package com.greencom.android.podcasts.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media2.session.SessionToken
import com.greencom.android.podcasts.player.MediaItemEpisode
import com.greencom.android.podcasts.player.PlayerServiceConnection
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.PLAYER_TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

// TODO
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection,
    private val repository: Repository,
) : ViewModel() {

    private val _event = Channel<MainActivityEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private val _isPlayerBottomSheetExpanded = MutableStateFlow(false)
    val isPlayerBottomSheetExpanded = _isPlayerBottomSheetExpanded.asStateFlow()

    private val _skipBackwardOrForwardValue = MutableStateFlow(0L)
    val skipBackwardOrForwardValue = _skipBackwardOrForwardValue.asStateFlow()

    val currentEpisode: StateFlow<MediaItemEpisode>
        get() = playerServiceConnection.currentEpisode

    val duration: StateFlow<Long>
        get() = playerServiceConnection.duration

    val currentPosition: StateFlow<Long>
        get() = playerServiceConnection.currentPosition

    val exoPlayerState: StateFlow<Int>
        get() = playerServiceConnection.exoPlayerState

    val isPlaying: StateFlow<Boolean>
        get() = playerServiceConnection.isPlaying

    fun play() {
        playerServiceConnection.play()
    }

    fun pause() {
        playerServiceConnection.pause()
    }

    fun seekTo(position: Long) {
        playerServiceConnection.seekTo(position)
    }

    fun increasePlaybackSpeed() {
        playerServiceConnection.increasePlaybackSpeed()
    }

    fun decreasePlaybackSpeed() {
        playerServiceConnection.decreasePlaybackSpeed()
    }

    @ExperimentalTime
    fun setSleepTimer(duration: Duration) {
        playerServiceConnection.setSleepTimer(duration)
    }

    fun removeSleepTimer() {
        playerServiceConnection.removeSleepTimer()
    }

    fun markCurrentEpisodeCompleted() {
        playerServiceConnection.markCurrentEpisodeCompleted()
    }

    fun setPlayerBottomSheetState(isExpanded: Boolean) {
        _isPlayerBottomSheetExpanded.value = isExpanded
    }

    fun resetPlayerBottomSheetState() {
        repeat(2) {
            _isPlayerBottomSheetExpanded.value = !_isPlayerBottomSheetExpanded.value
        }
    }

    fun updateSkipBackwardOrForwardValue(value: Long) {
        _skipBackwardOrForwardValue.value += value
    }

    fun resetSkipBackwardOrForwardValue() {
        _skipBackwardOrForwardValue.value = 0L
    }

    fun getPlaybackSpeed(): Flow<Float?> {
        return repository.getPlaybackSpeed()
    }

    fun getTheme(): Flow<Int?> = repository.getTheme()

    fun setExoPlayerState(state: Int) {
        playerServiceConnection.setExoPlayerState(state)
    }

    fun setIsPlaying(isPlaying: Boolean) {
        playerServiceConnection.setIsPlaying(isPlaying)
    }

    fun showPlayerOptionsDialog(episodeId: String?) {
        episodeId ?: return
        viewModelScope.launch {
            _event.send(MainActivityEvent.PlayerOptionsDialog(episodeId))
        }
    }

    fun navigateToEpisode(episodeId: String?) {
        episodeId ?: return
        viewModelScope.launch {
            _event.send(MainActivityEvent.NavigateToEpisode(episodeId))
        }
    }

    fun navigateToPodcast(podcastId: String?) {
        podcastId ?: return
        viewModelScope.launch {
            _event.send(MainActivityEvent.NavigateToPodcast(podcastId))
        }
    }

    @ExperimentalTime
    fun connectToPlayer(context: Context, sessionToken: SessionToken) {
        playerServiceConnection.connect(context, sessionToken)
    }

    fun disconnectFromPlayer() {
        playerServiceConnection.disconnect()
    }

    override fun onCleared() {
        Log.d(PLAYER_TAG, "MainActivityViewModel.onCleared()")
        super.onCleared()
        disconnectFromPlayer()
    }

    sealed class MainActivityEvent {
        data class PlayerOptionsDialog(val episodeId: String) : MainActivityEvent()
        data class NavigateToEpisode(val episodeId: String) : MainActivityEvent()
        data class NavigateToPodcast(val podcastId: String) : MainActivityEvent()
    }
}