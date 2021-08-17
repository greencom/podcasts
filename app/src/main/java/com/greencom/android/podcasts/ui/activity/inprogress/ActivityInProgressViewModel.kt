package com.greencom.android.podcasts.ui.activity.inprogress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.Player
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.player.PlayerServiceConnection
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    private var isCurrentEpisodePresented = false

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
                    isCurrentEpisodePresented = mIsCurrentEpisodeHere
                    return@combine mEpisodes
                }
                episodes
            }
            .combine(playerServiceConnection.exoPlayerState) { episodes, exoPlayerState ->
                if (episodes.isNotEmpty() && isCurrentEpisodePresented) {
                    return@combine episodes.map { episode ->
                        if (episode.isSelected && exoPlayerState == Player.STATE_BUFFERING) {
                            episode.copy(isBuffering = true)
                        } else {
                            episode
                        }
                    }
                }
                episodes
            }
            .combine(playerServiceConnection.isPlaying) { episodes, isPlaying ->
                if (episodes.isNotEmpty() && isCurrentEpisodePresented) {
                    return@combine episodes.map { episode ->
                        if (episode.isSelected && isPlaying) {
                            episode.copy(isPlaying = true)
                        } else {
                            episode
                        }
                    }
                }
                episodes
            }
            .flowOn(Dispatchers.Default)
            .collectLatest { episodes ->
                _uiState.value = when {
                    episodes.isNotEmpty() -> ActivityInProgressState.Success(episodes)
                    else -> ActivityInProgressState.Empty
                }
            }
    }

    /** Send `Play` command to the player. */
    fun play() {
        playerServiceConnection.play()
    }

    /** Send `Pause` command to the player. */
    fun pause() {
        playerServiceConnection.pause()
    }

    /** Set an episode to the player by episode ID and play. */
    fun setEpisode(episodeId: String) {
        playerServiceConnection.setEpisode(episodeId)
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

    /** Mark an episode as completed or reset its progress by ID. */
    fun onIsCompletedChange(episodeId: String, isCompleted: Boolean) {
        playerServiceConnection.onEpisodeIsCompletedChange(episodeId, isCompleted)
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