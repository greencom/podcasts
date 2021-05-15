package com.greencom.android.podcasts.ui.podcast

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.BaseViewModel
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(private val repository: Repository) : BaseViewModel() {

    private val _uiState = MutableStateFlow<PodcastState>(PodcastState.Loading)
    /** StateFlow of UI state. States are presented by [PodcastState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<PodcastEvent>(Channel.BUFFERED)
    /** Flow of events represented by [PodcastEvent]. */
    val event = _event.receiveAsFlow()

    // TODO
    private var isLoading = false

    /** Load a podcast for a given ID. The result will be posted to [uiState]. */
    fun getPodcast(id: String) = viewModelScope.launch {
        repository.getPodcast(id).collectLatest { state ->
            when (state) {
                is State.Loading -> _uiState.value = PodcastState.Loading
                is State.Success -> _uiState.value = PodcastState.Success(state.data)
                is State.Error -> _uiState.value = PodcastState.Error(state.exception)
            }
        }
    }

    /** Fetch the podcast from ListenAPI and insert it into the database. */
    fun fetchPodcast(id: String) = viewModelScope.launch {
        _event.send(PodcastEvent.Fetching)
        when (repository.fetchPodcast(id)) {
            is State.Error -> {
                _event.send(PodcastEvent.Snackbar(R.string.podcast_something_went_wrong))
            }

            // Make `when` expression exhaustive.
            is State.Loading -> {  }
            is State.Success -> {  }
        }
    }

    // TODO
    fun getEpisodes(id: String): Flow<List<Episode>> {
        return repository.getEpisodes(id)
    }

    // TODO
    fun fetchRecentEpisodes(id: String) = viewModelScope.launch {
        repository.fetchRecentEpisodes(id)
    }

    // TODO
    fun fetchMoreEpisodes(id: String, nextEpisodePubDate: Long) {
        if (!isLoading) {
            viewModelScope.launch {
                isLoading = true
                Timber.d("fetching")
                repository.fetchMoreEpisodes(id, nextEpisodePubDate)
                Timber.d("reset isLoading")
                isLoading = false
            }
        }
    }

    /**
     * Update subscription to a podcast by ID with a given value. If the value is
     * `false`, show UnsubscribeDialog to the user and wait for confirmation.
     */
    fun updateSubscription(id: String, subscribed: Boolean) = viewModelScope.launch {
        if (subscribed) {
            repository.updateSubscription(id, subscribed)
        } else {
            _event.send(PodcastEvent.UnsubscribeDialog(id))
        }
    }

    /**
     * Unsubscribe from a podcast by given podcast ID. Used only after UnsubscribeDialog
     * confirmation.
     */
    fun unsubscribe(id: String) = viewModelScope.launch {
        repository.updateSubscription(id, false)
    }

    /** Sealed class that represents the UI state of the [PodcastFragment]. */
    sealed class PodcastState {

        /** Represents a `Loading` state. */
        object Loading : PodcastState()

        /** Represents a `Success` state with a [Podcast] object. */
        data class Success(val podcast: Podcast) : PodcastState()

        /** Represents an `Error` state with a [Throwable] error. */
        data class Error(val error: Throwable) : PodcastState()
    }

    /** Sealed class that represents events of the [PodcastFragment]. */
    sealed class PodcastEvent {

        /** Represents a Snackbar event with a string res ID of the message to show. */
        data class Snackbar(@StringRes val stringRes: Int) : PodcastEvent()

        /** Represents a Fetching event triggered by [fetchPodcast] method. */
        object Fetching : PodcastEvent()

        /** Represents an UnsubscribeDialog event. */
        data class UnsubscribeDialog(val podcastId: String): PodcastEvent()
    }
}