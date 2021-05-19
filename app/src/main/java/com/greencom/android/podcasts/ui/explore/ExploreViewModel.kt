package com.greencom.android.podcasts.ui.explore

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.BaseViewModel
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel used by instances of [ExplorePageFragment]. */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: Repository) : BaseViewModel() {

    private val _uiState = MutableStateFlow<ExplorePageState>(ExplorePageState.Loading)
    /** StateFlow of UI states. States are presented by [ExplorePageState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<ExplorePageEvent>(Channel.BUFFERED)
    /** Flow of events. Events are presented by [ExplorePageEvent]. */
    val event = _event.receiveAsFlow()

    /** Load the best podcasts for a given genre ID. The result will be posted to [uiState]. */
    fun getBestPodcasts(genreId: Int) = viewModelScope.launch {
        repository.getBestPodcasts(genreId).collectLatest { state ->
            when (state) {
                is State.Loading -> _uiState.value = ExplorePageState.Loading
                is State.Success -> _uiState.value = ExplorePageState.Success(state.data)
                is State.Error -> _uiState.value = ExplorePageState.Error(state.exception)
            }
        }
    }

    /**
     * Fetch the best podcasts for a given genre ID from ListenAPI and insert them
     * into the database.
     */
    fun fetchBestPodcasts(genreId: Int) = viewModelScope.launch {
        _event.send(ExplorePageEvent.Fetching)
        when (repository.fetchBestPodcasts(genreId)) {
            is State.Error -> {
                _event.send(ExplorePageEvent.Snackbar(R.string.explore_best_podcasts_error))
            }

            // Make `when` expression exhaustive.
            is State.Loading -> {  }
            is State.Success -> {  }
        }
    }

    /** Refresh the best podcasts for a given genre ID. */
    fun refreshBestPodcasts(genreId: Int, currentList: List<PodcastShort>) = viewModelScope.launch {
        _event.send(ExplorePageEvent.Refreshing)
        when (repository.refreshBestPodcasts(genreId, currentList)) {
            is State.Success -> {
                _event.send(ExplorePageEvent.Snackbar(R.string.explore_podcasts_updated))
            }
            is State.Error -> {
                _event.send(ExplorePageEvent.Snackbar(R.string.explore_best_podcasts_error))
            }

            // Make `when` expression exhaustive.
            is State.Loading -> {  }
        }
    }

    /** Navigate to PodcastFragment with a given podcast ID. */
    fun navigateToPodcast(podcastId: String) = viewModelScope.launch {
        _event.send(ExplorePageEvent.NavigateToPodcast(podcastId))
    }

    /**
     * Update subscription to a podcast by ID with a given value. If the value is
     * `false`, show UnsubscribeDialog to the user and wait for confirmation.
     */
    fun updateSubscription(podcastId: String, subscribed: Boolean) = viewModelScope.launch {
        if (subscribed) {
            repository.updateSubscription(podcastId, subscribed)
        } else {
            _event.send(ExplorePageEvent.UnsubscribeDialog(podcastId))
        }
    }

    /**
     * Unsubscribe from a podcast by given podcast ID. Used only after UnsubscribeDialog
     * confirmation.
     */
    fun unsubscribe(podcastId: String) = viewModelScope.launch {
        repository.updateSubscription(podcastId, false)
    }

    /** Sealed class that represents the UI state of the [ExplorePageFragment]. */
    sealed class ExplorePageState {

        /** Represents a `Loading` state. */
        object Loading : ExplorePageState()

        /** Represents a `Success` state with a list of [PodcastShort] items. */
        data class Success(val podcasts: List<PodcastShort>) : ExplorePageState()

        /** Represents an `Error` state with a [Throwable] error. */
        data class Error(val error: Throwable) : ExplorePageState()
    }

    /** Sealed class that represents events of the [ExplorePageFragment]. */
    sealed class ExplorePageEvent {

        /** Represents an UnsubscribeDialog event. */
        data class UnsubscribeDialog(val podcastId: String): ExplorePageEvent()

        /** Navigate to a PodcastFragment with a given podcast ID. */
        data class NavigateToPodcast(val podcastId: String) : ExplorePageEvent()

        /** Represents a Snackbar event with a string res ID of the message to show. */
        data class Snackbar(@StringRes val stringRes: Int) : ExplorePageEvent()

        /** Represents a Fetching event triggered by [fetchBestPodcasts] method. */
        object Fetching : ExplorePageEvent()

        /** Represents a Refreshing event triggered by [refreshBestPodcasts] method. */
        object Refreshing : ExplorePageEvent()
    }
}