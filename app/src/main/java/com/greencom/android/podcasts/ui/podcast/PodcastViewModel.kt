package com.greencom.android.podcasts.ui.podcast

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media2.player.MediaPlayer
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.PodcastWithEpisodes
import com.greencom.android.podcasts.di.DispatcherModule.DefaultDispatcher
import com.greencom.android.podcasts.player.CurrentEpisode
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

/** ViewModel used by [PodcastFragment]. */
@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection,
    private val repository: Repository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    /** ID of the podcast associated with the fragment and ViewModel. */
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

    private val _showCompleted = MutableStateFlow(true)
    /** StateFlow that indicates whether or not completed episodes should be filtered. */
    val showCompleted = _showCompleted.asStateFlow()

    private val _isAppBarExpanded = MutableStateFlow(true)
    /**
     * StateFlow with the current [PodcastFragment]'s app bar state. `true` means the app bar
     * is expanded. Otherwise `false`.
     */
    val isAppBarExpanded = _isAppBarExpanded.asStateFlow()

    /** Job that handles episodes fetching. */
    private var episodesJob: Job? = null

    /** Are there episodes at the bottom of the list that should be loaded. */
    private var moreEpisodesNeededAtBottom = true

    /**
     * Is the currently selected episode one of the episodes of this podcast. If not,
     * [setCurrentEpisodeState] function will skip its body to save resources.
     */
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

    /** Reverse the [sortOrder] value and init episodes fetching. */
    fun changeSortOrder() {
        _sortOrder.value = sortOrder.value.reverse()
        // Initiate a new process of loading episodes.
        fetchEpisodes()
    }

    /** Change [showCompleted] value. */
    fun changeShowCompleted(showCompleted: Boolean) {
        _showCompleted.value = showCompleted
    }

    /** Load a podcast with episodes. The result will be posted to [uiState]. */
    fun getPodcastWithEpisodes() = viewModelScope.launch {
        repository.getPodcastWithEpisodes(podcastId)
            .combine(sortOrder) { flowState, sortOrder -> sortEpisodes(flowState, sortOrder) }
            .combine(showCompleted) { flowState, showCompleted ->
                filterCompleted(flowState, showCompleted)
            }
            .onEach(::checkBottomEpisodes)
            .flowOn(defaultDispatcher)
            .combine(playerServiceConnection.currentEpisode) { flowState, currentEpisode ->
                setCurrentEpisode(flowState, currentEpisode)
            }
            .combine(playerServiceConnection.playerState) { flowState, playerState ->
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
        if (result is State.Error) {
            _event.send(PodcastEvent.Snackbar(R.string.loading_error))
        }
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
                if (result is State.Error) {
                    _event.send(PodcastEvent.Snackbar(R.string.loading_error))
                }
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

    /** Show an [EpisodeOptionsDialog][com.greencom.android.podcasts.ui.dialogs.EpisodeOptionsDialog]. */
    fun showEpisodeOptions(episodeId: String, isEpisodeCompleted: Boolean) = viewModelScope.launch {
        _event.send(PodcastEvent.EpisodeOptionsDialog(episodeId, isEpisodeCompleted))
    }

    /** Add the episode to the bookmarks or remove from there. */
    fun updateEpisodeInBookmarks(episodeId: String, inBookmarks: Boolean) = viewModelScope.launch {
        repository.updateEpisodeInBookmarks(episodeId, inBookmarks)
    }

    /**
     * Unsubscribe from a podcast by given podcast ID. Used only after UnsubscribeDialog
     * confirmation.
     */
    fun unsubscribe(id: String) = viewModelScope.launch {
        repository.updateSubscription(id, false)
    }

    /** Mark an episode as completed or uncompleted by ID. */
    fun markEpisodeCompletedOrUncompleted(episodeId: String, isCompleted: Boolean) =
        viewModelScope.launch {
            repository.markEpisodeCompletedOrUncompleted(episodeId, isCompleted)
        }

    /** Navigate to EpisodeFragment with a given episode ID. */
    fun navigateToEpisode(episodeId: String) = viewModelScope.launch {
        _event.send(PodcastEvent.NavigateToEpisode(episodeId))
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

    /** Filter completed episodes depending on the [showCompleted] value. */
    private fun filterCompleted(
        flowState: State<PodcastWithEpisodes>,
        showCompleted: Boolean
    ): State<PodcastWithEpisodes> {
        if (flowState is State.Success && !showCompleted) {
            val episodes = flowState.data.episodes.filterNot { it.isCompleted }
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

    /** Check if there is the episode that is currently selected in the player. */
    private fun setCurrentEpisode(
        flowState: State<PodcastWithEpisodes>,
        currentEpisode: CurrentEpisode
    ): State<PodcastWithEpisodes> {
        if (flowState is State.Success) {
            var mIsCurrentEpisodeHere = false
            val episodes = flowState.data.episodes.map { episode ->
                if (episode.id == currentEpisode.id) {
                    mIsCurrentEpisodeHere = true
                    return@map episode.copy(isSelected = true)
                }
                episode
            }
            isCurrentEpisodeHere = mIsCurrentEpisodeHere
            return State.Success(PodcastWithEpisodes(flowState.data.podcast, episodes))
        }
        return flowState
    }

    /**
     * Set the appropriate state for the currently selected episode depending on the
     * current player state. Do nothing if the currently selected episode is not one of the
     * episodes of this podcast.
     */
    private fun setCurrentEpisodeState(
        flowState: State<PodcastWithEpisodes>,
        playerState: Int
    ): State<PodcastWithEpisodes> {
        if (flowState is State.Success && isCurrentEpisodeHere) {
            val episodes = flowState.data.episodes.map { episode ->
                if (episode.isSelected && playerState == MediaPlayer.PLAYER_STATE_PLAYING) {
                    episode.copy(isPlaying = true)
                } else {
                    episode
                }
            }
            return State.Success(PodcastWithEpisodes(flowState.data.podcast, episodes))
        }
        return flowState
    }

    /**
     * Set a new value to the [isAppBarExpanded] StateFlow.
     *
     * @param isExpanded whether the app bar is expanded or not.
     */
    fun setAppBarState(isExpanded: Boolean) {
        _isAppBarExpanded.value = isExpanded
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

        /** Navigate to EpisodeFragment with a given ID. */
        data class NavigateToEpisode(val episodeId: String) : PodcastEvent()

        /** Represents a Snackbar event with a string res ID of the message to show. */
        data class Snackbar(@StringRes val stringRes: Int) : PodcastEvent()

        /** Represents a Fetching event triggered by [fetchPodcast] method. */
        object Fetching : PodcastEvent()

        /** Represents an UnsubscribeDialog event. */
        data class UnsubscribeDialog(val podcastId: String) : PodcastEvent()

        /** Show an [EpisodeOptionsDialog]. */
        data class EpisodeOptionsDialog(val episodeId: String, val isEpisodeCompleted: Boolean) :
            PodcastEvent()

        /** Episodes fetching has started. */
        object EpisodesFetchingStarted : PodcastEvent()

        /** Episodes fetching has finished. */
        object EpisodesFetchingFinished : PodcastEvent()

        /** Episodes forced fetching initialized with swipe-to-refresh has finished. */
        object EpisodesForcedFetchingFinished : PodcastEvent()
    }
}