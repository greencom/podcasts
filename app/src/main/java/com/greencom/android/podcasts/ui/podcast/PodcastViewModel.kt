package com.greencom.android.podcasts.ui.podcast

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.PodcastWithEpisodes
import com.greencom.android.podcasts.di.DispatcherModule.DefaultDispatcher
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.BaseViewModel
import com.greencom.android.podcasts.utils.SortOrder
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val repository: Repository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<PodcastState>(PodcastState.Loading)
    /** StateFlow of UI state. States are presented by [PodcastState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<PodcastEvent>(Channel.BUFFERED)
    /** Flow of events represented by [PodcastEvent]. */
    val event = _event.receiveAsFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.RECENT_FIRST)
    /** StateFlow with the current [SortOrder] value. Defaults to [SortOrder.RECENT_FIRST]. */
    val sortOrder = _sortOrder.asStateFlow()

    var podcastId = ""

    /** Job that handles episodes fetching process init by [fetchEpisodes]. */
    private var fetchEpisodesJob: Job? = null

    /** Job that handles episodes fetching process process init by [fetchMoreEpisodes]. */
    private var fetchMoreEpisodesJob: Job? = null

    /** Indicates whether episodes are fetching by any function right now. */
    private var areEpisodesLoading = false

    /** Reverse the [sortOrder] value and init episodes fetching. */
    fun changeSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.RECENT_FIRST) {
            SortOrder.OLDEST_FIRST
        } else {
            SortOrder.RECENT_FIRST
        }
        fetchEpisodes()
    }

    /** Load a podcast with episodes. The result will be posted to [uiState]. */
    fun getPodcastWithEpisodes() = viewModelScope.launch {
        repository.getPodcastWithEpisodes(podcastId)
            .combine(sortOrder) { value, sortOrder -> sortEpisodes(value, sortOrder) }
            .flowOn(defaultDispatcher)
            .collectLatest { state ->
                when (state) {
                    is State.Loading -> _uiState.value = PodcastState.Loading
                    is State.Success -> _uiState.value = PodcastState.Success(state.data)
                    is State.Error -> _uiState.value = PodcastState.Error(state.exception)
                }
            }
    }

    /** Fetch the podcast from ListenAPI and insert it into the database. */
    fun fetchPodcast() = viewModelScope.launch {
        _event.send(PodcastEvent.Fetching)
        val result = repository.fetchPodcast(podcastId)
        if (result is State.Error) _event.send(PodcastEvent.Snackbar(R.string.loading_error))
    }

    /**
     * Fetch episodes for this podcast. Pass `true` to [isForced] to force the fetching regardless
     * the last update date.
     */
    fun fetchEpisodes(isForced: Boolean = false) {
        // Cancel all previous fetching processes.
        fetchEpisodesJob?.cancel()
        fetchMoreEpisodesJob?.cancel()

        fetchEpisodesJob = viewModelScope.launch {
            try {
                areEpisodesLoading = true
                val result = repository.fetchEpisodes(podcastId, sortOrder.value, isForced, _event)
                if (result is State.Error) _event.send(PodcastEvent.Snackbar(R.string.loading_error))
            } finally {
                areEpisodesLoading = false
                // Stop the appropriate loading indicator.
                if (isForced) {
                    _event.send(PodcastEvent.EpisodesForcedFetchingFinished)
                } else {
                    _event.send(PodcastEvent.EpisodesFetchingFinished)
                }
            }
        }
    }

    /** Use this function to fetch more episodes for this podcast on scroll. */
    fun fetchMoreEpisodes() {
        if (!areEpisodesLoading) {
            fetchMoreEpisodesJob = viewModelScope.launch {
                try {
                    areEpisodesLoading = true
                    repository.fetchMoreEpisodes(podcastId, sortOrder.value, _event)
                } finally {
                    areEpisodesLoading = false
                    _event.send(PodcastEvent.EpisodesFetchingFinished)
                }
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

    /** Sort episodes according to a given [sortOrder] value. */
    private fun sortEpisodes(
        value: State<PodcastWithEpisodes>,
        sortOrder: SortOrder
    ): State<PodcastWithEpisodes> {
        if (value is State.Success) {
            val episodes = if (sortOrder == SortOrder.RECENT_FIRST) {
                value.data.episodes.sortedByDescending { it.date }
            } else {
                value.data.episodes.sortedBy { it.date }
            }
            return State.Success(PodcastWithEpisodes(value.data.podcast, episodes))
        }
        return value
    }

    /** Sealed class that represents the UI state of the [PodcastFragment]. */
    sealed class PodcastState {

        /** Represents a `Loading` state. */
        object Loading : PodcastState()

        /** Represents a `Success` state with a [PodcastWithEpisodes] object. */
        data class Success(val podcastWithEpisodes: PodcastWithEpisodes) : PodcastState()

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
        data class UnsubscribeDialog(val podcastId: String) : PodcastEvent()

        /** Episodes fetching has started. */
        object EpisodesFetchingStarted : PodcastEvent()

        /** Episodes fetching has finished. */
        object EpisodesFetchingFinished : PodcastEvent()

        /** Episodes forced fetching initialized with swipe-to-refresh has finished. */
        object EpisodesForcedFetchingFinished : PodcastEvent()
    }
}