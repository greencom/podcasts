package com.greencom.android.podcasts.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel used by [PodcastSearch][com.greencom.android.podcasts.ui.search.SearchFragment]. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchState>(SearchState.EmptyQuery)
    /** StateFlow of UI state. States are represented by [SearchState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<SearchEvent>(Channel.BUFFERED)
    /** Flow of events represented by [SearchEvent]. */
    val event = _event.receiveAsFlow()

    /** Job that handles searching process. */
    private var searchJob: Job? = null

    /** Indicates when the keyboard should be shown when opening the SearchFragment. */
    var showKeyboard = true

    /**
     * Indicates when the search field should be selected when opening the SearchFragment with
     * restored search data.
     */
    var selectAll = false

    /** String query used for the last search. */
    var lastQuery = ""

    /** Offset that should be used to load more results for the same query. */
    var nextOffset = 0

    /** Search for a podcast with given arguments. Result will be posted to [uiState]. */
    fun search(query: String, offset: Int) {
        // If the another global search is active, do nothing.
        if (offset != 0 && searchJob?.isActive == true) return

        // If the query is empty, show an empty screen and return.
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = SearchState.EmptyQuery
            return
        }

        searchJob = viewModelScope.launch {
            lastQuery = query // Update last query.
            // If the current screen is empty, show Loading screen. Otherwise,
            // show a LinearProgressBar.
            if (uiState.value is SearchState.EmptyQuery) {
                _uiState.value = SearchState.Loading
            } else {
                _event.send(SearchEvent.StartProgressBar)
            }

            when (val result = repository.searchPodcast(query, offset)) {
                is State.Success -> {
                    // Check for "Nothing found".
                    if (result.data.total == 0) {
                        _uiState.value = SearchState.NothingFound
                        return@launch
                    }

                    _uiState.value = SearchState.Success(
                        query = result.data.query,
                        count = result.data.count,
                        total = result.data.total,
                        nextOffset = result.data.nextOffset,
                        podcasts = result.data.podcasts
                    )
                    nextOffset = result.data.nextOffset // Update next offset.
                }

                is State.Error -> {
                    _event.send(SearchEvent.CancelProgressBar)
                    // Show an Error screen only if there are no results for the same
                    // query on the screen.
                    if (offset == 0) {
                        _uiState.value = SearchState.Error(result.exception)
                    }
                }

                State.Loading -> {  }
            }
        }
    }

    /** Check fro the last search and restore it if possible. */
    fun restoreLastSearch() {
        val lastSearch = repository.getLastSearch() ?: return
        if (lastSearch.total == 0) return
        _uiState.value = SearchState.Success(
            query = lastSearch.query,
            count = lastSearch.count,
            total = lastSearch.total,
            nextOffset = lastSearch.nextOffset,
            podcasts = lastSearch.podcasts
        )
        lastQuery = lastSearch.query
        nextOffset = lastSearch.nextOffset
        // Select all text to give the user an opportunity to input new query easily.
        selectAll = true
    }

    /** Navigate to a podcast page bu a given ID. */
    fun navigateToPodcast(podcastId: String) = viewModelScope.launch {
        _event.send(SearchEvent.NavigateToPodcast(podcastId))
    }

    /** Sealed class that represents the UI state of the [SearchFragment]. */
    sealed class SearchState {

        /** Represents a `Empty query` state. */
        object EmptyQuery : SearchState()

        /** Represents a `Loading` state. */
        object Loading : SearchState()

        /** Represents a `Success` state with a search results. */
        data class Success(
            /** Search query. */
            val query: String,

            /** The number of search results in this page. */
            val count: Int,

            /** The total number of search results. */
            val total: Int,

            /**
             * Pass this value to the `offset` parameter of `searchPodcast()` to do
             * pagination of search results.
             */
            val nextOffset: Int,

            /** A list of search results. */
            val podcasts: List<Podcast>,
        ) : SearchState()

        /** Represents a `Nothing found` state. */
        object NothingFound : SearchState()

        /** Represents an `Error` state with a [Throwable] error. */
        data class Error(val error: Throwable) : SearchState()
    }

    /** Sealed class that represents events of the [SearchFragment]. */
    sealed class SearchEvent {

        /** Navigate to a podcast page with a given ID. */
        data class NavigateToPodcast(val podcastId: String) : SearchEvent()

        /** Start a LinearProgressBar animation. */
        object StartProgressBar : SearchEvent()

        /** Cancel a LinearProgressBar animation. */
        object CancelProgressBar : SearchEvent()
    }
}