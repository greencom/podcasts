package com.greencom.android.podcasts.ui.activity.bookmarks

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

/** ViewModel used by [ActivityBookmarksFragment]. */
@HiltViewModel
class ActivityBookmarksViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection,
    private val repository: Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityBookmarksState>(ActivityBookmarksState.Empty)
    
    /** StateFlow of UI state. States are represented by [ActivityBookmarksState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<ActivityBookmarksEvent>(Channel.BUFFERED)
    
    /** Flow of events represented by [ActivityBookmarksEvent]. */
    val event = _event.receiveAsFlow()

    /** Is the currently selected episode one of the episodes in the list. */
    private var isCurrentEpisodePresented = false

    /** Load a list of bookmarks. Result will be posted to [uiState]. */
    fun getBookmarks() = viewModelScope.launch {
        repository.getBookmarks()
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
                    episodes.isNotEmpty() -> ActivityBookmarksState.Success(episodes)
                    else -> ActivityBookmarksState.Empty
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

    /** Navigate to EpisodeFragment with a given ID. */
    fun navigateToEpisode(episodeId: String) = viewModelScope.launch {
        _event.send(ActivityBookmarksEvent.NavigateToEpisode(episodeId))
    }

    /** Add an episode to the bookmarks or remove from there. */
    fun onInBookmarksChange(episodeId: String, inBookmarks: Boolean) = viewModelScope.launch {
        repository.onEpisodeInBookmarksChange(episodeId, inBookmarks)
    }

    /**
     * Show an [EpisodeOptionsDialog][com.greencom.android.podcasts.ui.dialogs.EpisodeOptionsDialog].
     */
    fun showEpisodeOptions(episodeId: String, isEpisodeCompleted: Boolean) = viewModelScope.launch {
        _event.send(ActivityBookmarksEvent.EpisodeOptionDialog(episodeId, isEpisodeCompleted))
    }

    /** Mark an episode as completed or reset its progress by ID. */
    fun onIsCompletedChange(episodeId: String, isCompleted: Boolean) =
        viewModelScope.launch {
            repository.onEpisodeIsCompletedChange(episodeId, isCompleted)
        }

    /** Sealed class that represents the UI state of the [ActivityBookmarksFragment]. */
    sealed class ActivityBookmarksState {

        /** Empty screen. */
        object Empty : ActivityBookmarksState()

        /** Success screen with a list of episodes. */
        data class Success(val episodes: List<Episode>) : ActivityBookmarksState()
    }

    /** Sealed class that represents events of the [ActivityBookmarksFragment]. */
    sealed class ActivityBookmarksEvent {

        /** Navigate to the episode page by ID. */
        data class NavigateToEpisode(val episodeId: String) : ActivityBookmarksEvent()

        /**
         * Show an [EpisodeOptionsDialog][com.greencom.android.podcasts.ui.dialogs.EpisodeOptionsDialog].
         */
        data class EpisodeOptionDialog(val episodeId: String, val isEpisodeCompleted: Boolean) :
            ActivityBookmarksEvent()
    }
}