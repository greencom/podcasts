package com.greencom.android.podcasts.ui.activity.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastEvent.EpisodeOptionsDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel used by [ActivityHistoryFragment]. */
@HiltViewModel
class ActivityHistoryViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ActivityHistoryState>(ActivityHistoryState.Empty)
    /** StateFlow of UI state. States are presented by [ActivityHistoryState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<ActivityHistoryEvent>(Channel.BUFFERED)
    /** Flow of events represented by [ActivityHistoryEvent]. */
    val event = _event.receiveAsFlow()

    /** Load a history of completed episodes. Result will be posted to [uiState]. */
    fun getEpisodeHistory() = viewModelScope.launch {
        repository.getEpisodeHistory().collectLatest { episodes ->
            _uiState.value = when {
                episodes.isNotEmpty() -> ActivityHistoryState.Success(episodes)
                else -> ActivityHistoryState.Empty
            }
        }
    }

    /** Navigate to EpisodeFragment with given ID. */
    fun navigateToEpisode(episodeId: String) = viewModelScope.launch {
        _event.send(ActivityHistoryEvent.NavigateToEpisode(episodeId))
    }

    /** Show an [EpisodeOptionsDialog]. */
    fun showEpisodeOptions(episodeId: String, isEpisodeCompleted: Boolean) = viewModelScope.launch {
        _event.send(ActivityHistoryEvent.EpisodeOptionDialog(episodeId, isEpisodeCompleted))
    }

    /** Mark an episode as completed or uncompleted by ID. */
    fun markEpisodeCompletedOrUncompleted(episodeId: String, isCompleted: Boolean) =
        viewModelScope.launch {
            repository.markEpisodeCompletedOrUncompleted(episodeId, isCompleted)
        }

    /** Sealed class that represents the UI state of the [ActivityHistoryFragment]. */
    sealed class ActivityHistoryState {

        /** Empty screen. */
        object Empty : ActivityHistoryState()

        /** Success screen with a list of episodes. */
        data class Success(val episodes: List<Episode>) : ActivityHistoryState()
    }

    /** Sealed class that represents events of the [ActivityHistoryFragment]. */
    sealed class ActivityHistoryEvent {

        /** Event to navigate to EpisodeFragment with given ID. */
        data class NavigateToEpisode(val episodeId: String) : ActivityHistoryEvent()

        /** Show an [EpisodeOptionsDialog]. */
        data class EpisodeOptionDialog(val episodeId: String, val isEpisodeCompleted: Boolean) :
                ActivityHistoryEvent()
    }
}