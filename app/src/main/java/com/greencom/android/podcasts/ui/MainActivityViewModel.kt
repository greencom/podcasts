package com.greencom.android.podcasts.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media2.session.SessionToken
import com.greencom.android.podcasts.player.MediaItemEpisode
import com.greencom.android.podcasts.player.PlayerServiceConnection
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/** ViewModel used by [MainActivity]. */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection,
    private val repository: Repository,
) : ViewModel() {

    private val _event = Channel<MainActivityEvent>(Channel.BUFFERED)

    /** Flow of [MainActivityEvent]s. */
    val event = _event.receiveAsFlow()

    private val _isPlayerBottomSheetExpanded = MutableStateFlow(false)

    /** Whether the player bottom sheet is expanded or not. */
    val isPlayerBottomSheetExpanded = _isPlayerBottomSheetExpanded.asStateFlow()

    private val _seekBackwardOrForwardValue = MutableStateFlow(0L)

    /** The current 'seek backward or forward' value. */
    val seekBackwardOrForwardValue = _seekBackwardOrForwardValue.asStateFlow()

    /** StateFlow with the current player episode. */
    val currentEpisode: StateFlow<MediaItemEpisode>
        get() = playerServiceConnection.currentEpisode

    /** StateFlow with the current episode duration. */
    val duration: StateFlow<Long>
        get() = playerServiceConnection.duration

    /** StateFlow with the current player position. */
    val currentPosition: StateFlow<Long>
        get() = playerServiceConnection.currentPosition

    /** StateFlow with the current ExoPlayer state. */
    val exoPlayerState: StateFlow<Int>
        get() = playerServiceConnection.exoPlayerState

    /** StateFlow with the current ExoPlayer `isPlaying` state. */
    val isPlaying: StateFlow<Boolean>
        get() = playerServiceConnection.isPlaying

    /** Connect to the player using [PlayerServiceConnection]. */
    @ExperimentalTime
    fun connectToPlayer(context: Context, sessionToken: SessionToken) {
        playerServiceConnection.connect(context, sessionToken)
    }

    /** Disconnect the [PlayerServiceConnection] from the player. */
    fun disconnectFromPlayer() {
        playerServiceConnection.disconnect()
    }

    /** Set [PlayerServiceConnection.exoPlayerState] value. */
    fun setExoPlayerState(state: Int) {
        playerServiceConnection.setExoPlayerState(state)
    }

    /** Set [PlayerServiceConnection.isPlaying] value. */
    fun setIsPlaying(isPlaying: Boolean) {
        playerServiceConnection.setIsPlaying(isPlaying)
    }

    /** Send `Play` command to the player. */
    fun play() {
        playerServiceConnection.play()
    }

    /** Send `Pause` command to the player. */
    fun pause() {
        playerServiceConnection.pause()
    }

    /** Seek the episode to a given [position] in ms. */
    fun seekTo(position: Long) {
        playerServiceConnection.seekTo(position)
    }

    /** Increase the playback speed. Four modes are available: `1.0`, `1.2`, `1.5` and `2.0`. */
    fun increasePlaybackSpeed() {
        playerServiceConnection.increasePlaybackSpeed()
    }

    /** Decrease the playback speed. Four modes are available: `1.0`, `1.2`, `1.5` and `2.0`. */
    fun decreasePlaybackSpeed() {
        playerServiceConnection.decreasePlaybackSpeed()
    }

    /**
     * Set a Sleep Timer for a given [Duration]. Only one timer can be active at the same time.
     */
    @ExperimentalTime
    fun setSleepTimer(duration: Duration) {
        playerServiceConnection.setSleepTimer(duration)
    }

    /** Remove the active Sleep Timer. Do nothing if there is no active Sleep Timer. */
    fun removeSleepTimer() {
        playerServiceConnection.removeSleepTimer()
    }

    /** Mark the current episode as completed and close the player. */
    fun markCurrentEpisodeCompleted(episodeId: String) {
        playerServiceConnection.onEpisodeIsCompletedChange(episodeId, true)
    }

    /** Update [isPlayerBottomSheetExpanded] with a given value. */
    fun setPlayerBottomSheetState(isExpanded: Boolean) {
        _isPlayerBottomSheetExpanded.value = isExpanded
    }

    /** Update [seekBackwardOrForwardValue] with a given value. */
    fun updateSeekBackwardOrForwardValue(value: Long) {
        _seekBackwardOrForwardValue.value += value
    }

    /** Set [seekBackwardOrForwardValue] to `0`. */
    fun resetSeekBackwardOrForwardValue() {
        _seekBackwardOrForwardValue.value = 0L
    }

    /** Get a Flow with a player playback speed. */
    fun getPlaybackSpeed(): Flow<Float?> {
        return repository.getPlaybackSpeed()
    }

    /** Get a Flow with the app theme mode. */
    fun getTheme(): Flow<Int?> = repository.getTheme()

    /** Show [PlayerOptionsDialog][com.greencom.android.podcasts.ui.dialogs.PlayerOptionsDialog]. */
    fun showPlayerOptionsDialog(episodeId: String?) {
        episodeId ?: return
        viewModelScope.launch {
            _event.send(MainActivityEvent.PlayerOptionsDialog(episodeId))
        }
    }

    /** Navigate to the episode page. */
    fun navigateToEpisode(episodeId: String?) {
        episodeId ?: return
        viewModelScope.launch {
            _event.send(MainActivityEvent.NavigateToEpisode(episodeId))
        }
    }

    /** Navigate to the podcast page. */
    fun navigateToPodcast(podcastId: String?) {
        podcastId ?: return
        viewModelScope.launch {
            _event.send(MainActivityEvent.NavigateToPodcast(podcastId))
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Disconnect PlayerServiceConnection.
        disconnectFromPlayer()
    }

    /** Sealed class that represents [MainActivity] events. */
    sealed class MainActivityEvent {

        /**
         * Show [PlayerOptionsDialog][com.greencom.android.podcasts.ui.dialogs.PlayerOptionsDialog].
         */
        data class PlayerOptionsDialog(val episodeId: String) : MainActivityEvent()

        /** Navigate to the episode page. */
        data class NavigateToEpisode(val episodeId: String) : MainActivityEvent()

        /** Navigate to the podcast page. */
        data class NavigateToPodcast(val podcastId: String) : MainActivityEvent()
    }
}