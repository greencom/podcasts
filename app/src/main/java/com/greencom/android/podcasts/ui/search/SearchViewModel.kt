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

    private val _uiState = MutableStateFlow<SearchState>(SearchState.EmptyQuery)
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<SearchEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private var searchJob: Job? = null

    var showKeyboard = true

    var selectAll = false

    var lastQuery = ""

    fun search(query: String, offset: Int) {
        if (offset != 0 && searchJob?.isActive == true) return

        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = SearchState.EmptyQuery
            return
        }

        searchJob = viewModelScope.launch {
            lastQuery = query
            if (uiState.value is SearchState.EmptyQuery) {
                _uiState.value = SearchState.Loading
            } else {
                _event.send(SearchEvent.StartProgressBar)
            }

            when (val result = repository.searchPodcast(query, offset)) {
                is State.Success -> {
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
                }

                is State.Error -> {
                    _event.send(SearchEvent.CancelProgressBar)
                    if (offset == 0) {
                        _uiState.value = SearchState.Error(result.exception)
                    }
                }

                State.Loading -> {  }
            }
        }
    }

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
        selectAll = true
    }

    fun navigateToPodcast(podcastId: String) = viewModelScope.launch {
        _event.send(SearchEvent.NavigateToPodcast(podcastId))
    }

    sealed class SearchState {

        object EmptyQuery : SearchState()

        object Loading : SearchState()

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

        object NothingFound : SearchState()

        data class Error(val error: Throwable) : SearchState()
    }

    sealed class SearchEvent {

        data class NavigateToPodcast(val podcastId: String) : SearchEvent()

        object StartProgressBar : SearchEvent()

        object CancelProgressBar : SearchEvent()
    }
}