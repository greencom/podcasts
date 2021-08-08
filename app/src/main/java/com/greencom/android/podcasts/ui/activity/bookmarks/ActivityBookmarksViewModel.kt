package com.greencom.android.podcasts.ui.activity.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media2.player.MediaPlayer
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.player.PlayerServiceConnection
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastEvent.EpisodeOptionsDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel used by [ActivityBookmarksFragment]. */
@HiltViewModel
class ActivityBookmarksViewModel @Inject constructor(
    private val repository: Repository,
    private val playerServiceConnection: PlayerServiceConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityBookmarksState>(ActivityBookmarksState.Empty)
    /** StateFlow of UI state. States are represented by [ActivityBookmarksState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<ActivityBookmarksEvent>(Channel.BUFFERED)
    /** Flow of events represented by [ActivityBookmarksEvent]. */
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
                    episodes.isNotEmpty() -> ActivityBookmarksState.Success(episodes)
                    else -> ActivityBookmarksState.Empty
                }
            }
    }

    /** Navigate to EpisodeFragment with given ID. */
    fun navigateToEpisode(episodeId: String) = viewModelScope.launch {
        _event.send(ActivityBookmarksEvent.NavigateToEpisode(episodeId))
    }

    /** Remove the episode from the bookmarks. */
    fun removeFromBookmarks(episodeId: String) = viewModelScope.launch {
        repository.updateEpisodeInBookmarks(episodeId, false)
    }

    /** Show an [EpisodeOptionsDialog]. */
    fun showEpisodeOptions(episodeId: String, isEpisodeCompleted: Boolean) = viewModelScope.launch {
        _event.send(ActivityBookmarksEvent.EpisodeOptionDialog(episodeId, isEpisodeCompleted))
    }

    /** Mark an episode as completed or uncompleted by ID. */
    fun markEpisodeCompletedOrUncompleted(episodeId: String, isCompleted: Boolean) =
        viewModelScope.launch {
            repository.markEpisodeCompletedOrUncompleted(episodeId, isCompleted)
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

        /** Navigate to episode page. */
        data class NavigateToEpisode(val episodeId: String) : ActivityBookmarksEvent()

        /** Show an [EpisodeOptionsDialog]. */
        data class EpisodeOptionDialog(val episodeId: String, val isEpisodeCompleted: Boolean) :
            ActivityBookmarksEvent()
    }
}