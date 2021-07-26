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

// TODO
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchState>(SearchState.QueryIsEmpty)
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<SearchEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private var searchJob: Job? = null

    var showKeyboard = true

    var selectAll = false

    fun checkLastSearch() {
        val lastSearch = repository.getLastSearch()
        if (lastSearch != null) {
            _uiState.value = SearchState.LastSearch(
                query = lastSearch.query,
                count = lastSearch.result.count,
                total = lastSearch.result.total,
                nextOffset = lastSearch.result.nextOffset,
                podcasts = lastSearch.result.podcasts
            )
        }
    }

    fun search(query: String, sortByDate: Boolean, offset: Int) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = SearchState.QueryIsEmpty
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.value = SearchState.Loading
            when (val result = repository.searchPodcast(query, sortByDate, offset)) {
                is State.Success -> {
                    if (result.data.total == 0) {
                        _uiState.value = SearchState.NoResultsFound
                        return@launch
                    }

                    _uiState.value = SearchState.Success(
                        count = result.data.count,
                        total = result.data.total,
                        nextOffset = result.data.nextOffset,
                        podcasts = result.data.podcasts
                    )
                }
                is State.Error -> _uiState.value = SearchState.Error(result.exception)
                is State.Loading -> {  }
            }
        }
    }

    fun navigateToPodcast(podcastId: String) = viewModelScope.launch {
        _event.send(SearchEvent.NavigateToPodcast(podcastId))
    }

    sealed class SearchState {

        object QueryIsEmpty : SearchState()

        data class LastSearch(
            /** Last search query. */
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

            /** Last search result. */
            val podcasts: List<Podcast>
        ) : SearchState()

        object Loading : SearchState()

        data class Success(
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

        data class Error(val error: Throwable) : SearchState()

        object NoResultsFound : SearchState()
    }

    sealed class SearchEvent {

        data class NavigateToPodcast(val podcastId: String) : SearchEvent()
    }
}