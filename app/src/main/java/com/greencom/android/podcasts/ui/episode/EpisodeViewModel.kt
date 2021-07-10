package com.greencom.android.podcasts.ui.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.player.PlayerServiceConnection
import com.greencom.android.podcasts.player.isPlayerPlaying
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO
@HiltViewModel
class EpisodeViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection,
    private val repository: Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EpisodeState>(EpisodeState.Loading)
    /** StateFlow of UI state. States are presented by [EpisodeState]. */
    val uiState = _uiState.asStateFlow()

    private val _isAppBarExpanded = MutableStateFlow(true)
    /**
     * StateFlow with the current [EpisodeFragment]'s app bar state. `true` means the app bar
     * is expanded. Otherwise `false`.
     */
    val isAppBarExpanded = _isAppBarExpanded.asStateFlow()

    /** Load an episode for a given ID. The result will be posted to [uiState]. */
    fun getEpisode(episodeId: String) = viewModelScope.launch {
        repository.getEpisode(episodeId)
            .combine(playerServiceConnection.currentEpisode) { flowState, currentEpisode ->
                return@combine if (flowState is State.Success && flowState.data.id == currentEpisode.id) {
                    State.Success(flowState.data.copy(isSelected = true))
                } else {
                    flowState
                }
            }
            .combine(playerServiceConnection.playerState) { flowState, playerState ->
                return@combine if (flowState is State.Success && flowState.data.isSelected &&
                    playerState.isPlayerPlaying()
                ) {
                    State.Success(flowState.data.copy(isPlaying = true))
                } else {
                    flowState
                }
            }
            .collectLatest { state ->
                when (state) {
                    is State.Loading -> _uiState.value = EpisodeState.Loading
                    is State.Success -> _uiState.value = EpisodeState.Success(state.data)
                    is State.Error -> _uiState.value = EpisodeState.Error(state.exception)
                }
            }
    }

    // TODO
    fun play() {
        playerServiceConnection.play()
    }

    // TODO()
    fun pause() {
        playerServiceConnection.pause()
    }

    // TODO
    fun playEpisode(episodeId: String) {
        playerServiceConnection.playEpisode(episodeId)
    }

    // TODO
    fun playFromTimecode(episodeId: String, timecode: Long) {
        playerServiceConnection.playFromTimecode(episodeId, timecode)
    }

    /**
     * Set a new value to the [isAppBarExpanded] StateFlow.
     *
     * @param isExpanded whether the app bar is expanded or not.
     */
    fun setAppBarState(isExpanded: Boolean) {
        _isAppBarExpanded.value = isExpanded
    }

    /** Sealed class that represents the UI state of the [EpisodeFragment]. */
    sealed class EpisodeState {

        /** Represents a `Loading` state. */
        object Loading : EpisodeState()

        /** Represents a `Success` state with an [Episode] object. */
        data class Success(val episode: Episode) : EpisodeState()

        /** Represents an `Error` state with a [Throwable] error. */
        data class Error(val error: Throwable) : EpisodeState()
    }
}