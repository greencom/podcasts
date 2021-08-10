package com.greencom.android.podcasts.ui.activity.inprogress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media2.player.MediaPlayer
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.player.PlayerServiceConnection
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel used by [ActivityInProgressFragment]. */
@HiltViewModel
class ActivityInProgressViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection,
    private val repository: Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityInProgressState>(ActivityInProgressState.Empty)
    /** StateFlow of UI state. States are represented by [ActivityInProgressState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<ActivityInProgressEvent>(Channel.BUFFERED)
    /** Flow of events represented by [ActivityInProgressEvent]. */
    val event = _event.receiveAsFlow()

    /** Is the currently selected episode one of the episodes in the bookmarks. */
    private var isCurrentEpisodeHere = false

    // TODO
    fun playEpisode(episodeId: String) {
        playerServiceConnection.playEpisode(episodeId)
    }

    // TODO
    fun play() {
        playerServiceConnection.play()
    }

    // TODO
    fun pause() {
        playerServiceConnection.pause()
    }

    /** Load a list of episodes in progress. Result will be posted to [uiState]. */
    fun getEpisodesInProgress() = viewModelScope.launch {
        repository.getEpisodesInProgress()
            .combine(playerServiceConnection.currentEpisode) { episodes, currentEpisode ->
                if (episodes.isNotEmpty()) {
                    var mIsCurrentEpisodeHere = false
                    val mEpisodes = episodes.map { episode ->
                        if (episode.id == currentEpisode.id) {
                            mIsCurrentEpisodeHere = true
                            return@map episode.copy(isSelected = true)
                        }
                        episode
                    }
                    isCurrentEpisodeHere = mIsCurrentEpisodeHere
                    return@combine mEpisodes
                }
                return@combine episodes
            }
            .combine(playerServiceConnection.playerState) { episodes, playerState ->
                if (episodes.isNotEmpty() && isCurrentEpisodeHere) {
                    return@combine episodes.map { episode ->
                        if (episode.isSelected && playerState == MediaPlayer.PLAYER_STATE_PLAYING) {
                            episode.copy(isPlaying = true)
                        } else {
                            episode
                        }
                    }
                }
                return@combine episodes
            }
            .collectLatest { episodes ->
                _uiState.value = when {
                    episodes.isNotEmpty() -> ActivityInProgressState.Success(episodes)
                    else -> ActivityInProgressState.Empty
                }
            }
    }

    /** Navigate to EpisodeFragment with given ID. */
    fun navigateToEpisode(episodeId: String) = viewModelScope.launch {
        _event.send(ActivityInProgressEvent.NavigateToEpisode(episodeId))
    }

    /** Add an episode to the bookmarks or remove from there. */
    fun onInBookmarksChange(episodeId: String, inBookmarks: Boolean) = viewModelScope.launch {
        repository.onEpisodeInBookmarksChange(episodeId, inBookmarks)
    }

    /**
     * Show an [EpisodeOptionsDialog][com.greencom.android.podcasts.ui.dialogs.EpisodeOptionsDialog].
     */
    fun showEpisodeOptions(episodeId: String, isEpisodeCompleted: Boolean) = viewModelScope.launch {
        _event.send(ActivityInProgressEvent.EpisodeOptionDialog(episodeId, isEpisodeCompleted))
    }

    /** Mark an episode as completed or uncompleted by ID. */
    fun onIsCompletedChange(episodeId: String, isCompleted: Boolean) =
        viewModelScope.launch {
            repository.onEpisodeIsCompletedChange(episodeId, isCompleted)
        }

    /** Sealed class that represents the UI state of the [ActivityInProgressFragment]. */
    sealed class ActivityInProgressState {

        /** Empty screen. */
        object Empty : ActivityInProgressState()

        /** Success screen with a list of episodes. */
        data class Success(val episode: List<Episode>) : ActivityInProgressState()
    }

    /** Sealed class that represents events of the [ActivityInProgressFragment]. */
    sealed class ActivityInProgressEvent {

        /** Navigate to episode page. */
        data class NavigateToEpisode(val episodeId: String) : ActivityInProgressEvent()

        /**
         * Show an [EpisodeOptionsDialog][com.greencom.android.podcasts.ui.dialogs.EpisodeOptionsDialog].
         */
        data class EpisodeOptionDialog(val episodeId: String, val isEpisodeCompleted: Boolean) :
            ActivityInProgressEvent()
    }
}