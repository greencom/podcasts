package com.greencom.android.podcasts.ui.podcast

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media2.player.MediaPlayer
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.PodcastWithEpisodes
import com.greencom.android.podcasts.di.DispatcherModule.DefaultDispatcher
import com.greencom.android.podcasts.player.PlayerServiceConnection
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.SortOrder
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val player: PlayerServiceConnection,
    private val repository: Repository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    var podcastId = ""

    private val _uiState = MutableStateFlow<PodcastState>(PodcastState.Loading)
    /** StateFlow of UI state. States are presented by [PodcastState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<PodcastEvent>(Channel.BUFFERED)
    /** Flow of events represented by [PodcastEvent]. */
    val event = _event.receiveAsFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.RECENT_FIRST)
    /** StateFlow with the current [SortOrder] value. Defaults to [SortOrder.RECENT_FIRST]. */
    val sortOrder = _sortOrder.asStateFlow()

    /** Job that handles episodes fetching. */
    private var episodesJob: Job? = null

    /** Are there episodes at the bottom of a list that should be loaded. */
    private var moreEpisodesNeededAtBottom = true

    // TODO
    @ExperimentalTime
    fun playEpisode(episodes: Episode) {
        player.playEpisode(episodes)
    }

    // TODO
    fun play() {
        player.play()
    }

    // TODO
    fun pause() {
        player.pause()
    }

    /** Reverse the [sortOrder] value and init episodes fetching. */
    fun changeSortOrder() {
        _sortOrder.value = sortOrder.value.reverse()
        // Initiate a new process of loading episodes.
        fetchEpisodes()
    }

    /** Load a podcast with episodes. The result will be posted to [uiState]. */
    fun getPodcastWithEpisodes() = viewModelScope.launch {
        repository.getPodcastWithEpisodes(podcastId)
            .combine(sortOrder) { flowState, sortOrder -> sortEpisodes(flowState, sortOrder) }
            .onEach(::checkBottomEpisodes)
            .flowOn(defaultDispatcher)
            .combine(player.currentEpisode) { flowState, currentEpisode ->
                setCurrentEpisode(flowState, currentEpisode)
            }
            .combine(player.playerState) { flowState, playerState ->
                setCurrentEpisodeState(flowState, playerState)
            }
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
     * the last update date. The previous [episodesJob] will be canceled.
     */
    fun fetchEpisodes(isForced: Boolean = false) {
        episodesJob?.cancel()
        episodesJob = viewModelScope.launch {
            try {
                val result = repository.fetchEpisodes(podcastId, sortOrder.value, isForced, _event)
                if (result is State.Error) _event.send(PodcastEvent.Snackbar(R.string.loading_error))
            } finally {
                // Stop the appropriate loading indicator.
                if (isForced) {
                    _event.send(PodcastEvent.EpisodesForcedFetchingFinished)
                } else {
                    _event.send(PodcastEvent.EpisodesFetchingFinished)
                }
            }
        }
    }

    /**
     * Use this function to fetch more episodes for this podcast on scroll. Fetching
     * will not start if [episodesJob] is active.
     */
    fun fetchMoreEpisodes() {
        if (moreEpisodesNeededAtBottom && episodesJob?.isActive == false) {
            episodesJob = viewModelScope.launch {
                try {
                    repository.fetchMoreEpisodes(podcastId, sortOrder.value, _event)
                } finally {
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
        flowState: State<PodcastWithEpisodes>,
        sortOrder: SortOrder
    ): State<PodcastWithEpisodes> {
        if (flowState is State.Success) {
            val episodes = when (sortOrder) {
                SortOrder.RECENT_FIRST -> flowState.data.episodes.sortedByDescending { it.date }
                SortOrder.OLDEST_FIRST -> flowState.data.episodes.sortedBy { it.date }
            }
            return State.Success(PodcastWithEpisodes(flowState.data.podcast, episodes))
        }
        return flowState
    }

    /**
     * Check if there are episodes at the bottom of the list that should be loaded.
     * Result will be posted to [moreEpisodesNeededAtBottom].
     */
    private fun checkBottomEpisodes(state: State<PodcastWithEpisodes>) {
        if (state is State.Success) {
            // Return if the list is empty.
            if (state.data.episodes.isEmpty()) return

            // Get the latest or earliest podcast pub date depending on the current sort order.
            // This date represents the date of the episode that should be the last in the list
            // for the current sort order.
            val bottomPubDate = when (sortOrder.value) {
                SortOrder.RECENT_FIRST -> state.data.podcast.earliestPubDate
                SortOrder.OLDEST_FIRST -> state.data.podcast.latestPubDate
            }

            // Compare the last loaded episode pub date and bottomPubDate.
            moreEpisodesNeededAtBottom = state.data.episodes.last().date != bottomPubDate
        }
    }

    // TODO
    private fun setCurrentEpisode(
        flowState: State<PodcastWithEpisodes>,
        currentEpisode: PlayerServiceConnection.CurrentEpisode
    ): State<PodcastWithEpisodes> {
        if (flowState is State.Success) {
            val episodes = flowState.data.episodes.map { episode ->
                if (episode.id == currentEpisode.id) {
                    return@map episode.copy(isSelected = true)
                }
                episode
            }
            return State.Success(PodcastWithEpisodes(flowState.data.podcast, episodes))
        }
        return flowState
    }

    // TODO
    private fun setCurrentEpisodeState(
        flowState: State<PodcastWithEpisodes>,
        playerState: Int
    ): State<PodcastWithEpisodes> {
        if (flowState is State.Success) {
            val episodes = flowState.data.episodes.map { episode ->
                if (episode.isSelected) {
                    return@map when (playerState) {
                        MediaPlayer.PLAYER_STATE_PLAYING -> episode.copy(isPlaying = true)
                        MediaPlayer.PLAYER_STATE_PAUSED -> {
                            if (player.currentPosition.value > 0) {
                                episode.copy(position = player.currentPosition.value)
                            } else {
                                episode
                            }
                        }
                        else -> episode
                    }
                }
                episode
            }
            return State.Success(PodcastWithEpisodes(flowState.data.podcast, episodes))
        }
        return flowState
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