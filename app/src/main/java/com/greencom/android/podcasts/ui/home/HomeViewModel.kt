package com.greencom.android.podcasts.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel used by [HomeFragment]. */
@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeState>(HomeState.Empty)

    /** StateFlow of UI state. States are represented by [HomeState]. */
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<HomeEvent>(Channel.BUFFERED)

    /** Flow of events represented by [HomeEvent]. */
    val event = _event.receiveAsFlow()

    /** Load subscriptions. The result will be posted to [uiState]. */
    fun getSubscriptions() = viewModelScope.launch {
        repository.getSubscriptions().collectLatest { podcasts ->
            _uiState.value = if (podcasts.isNotEmpty()) {
                HomeState.Success(podcasts)
            } else {
                HomeState.Empty
            }
        }
    }

    /** Get a Flow of the subscription presentation mode. */
    fun getSubscriptionMode(): Flow<Int?> = repository.getSubscriptionMode()

    /** Unsubscribe from the podcast by ID. */
    fun unsubscribe(podcastId: String) = viewModelScope.launch {
        repository.onPodcastSubscribedChange(podcastId, false)
    }

    /** Navigate to a PodcastFragment by ID. */
    fun navigateToPodcast(podcastId: String) = viewModelScope.launch {
        _event.send(HomeEvent.NavigateToPodcast(podcastId))
    }

    /** Show an [UnsubscribeDialog][com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog]. */
    fun showUnsubscribeDialog(podcastId: String) = viewModelScope.launch {
        _event.send(HomeEvent.UnsubscribeDialog(podcastId))
    }

    /** Sealed class that represents the UI state of the [HomeFragment]. */
    sealed class HomeState {

        /** Empty screen. */
        object Empty : HomeState()

        /** Represents a `Success` state with a list of [PodcastShort] the user subscribed to. */
        data class Success(val podcasts: List<PodcastShort>) : HomeState()
    }

    /** Sealed class that represents events of the [HomeFragment]. */
    sealed class HomeEvent {

        /** Navigate to a podcast page by ID. */
        data class NavigateToPodcast(val podcastId: String) : HomeEvent()

        /** Show an [UnsubscribeDialog]. */
        data class UnsubscribeDialog(val podcastId: String) : HomeEvent()
    }
}