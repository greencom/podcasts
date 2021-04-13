package com.greencom.android.podcasts.ui.explore

import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

/** ViewModel used by instances of [ExplorePageFragment]. */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: Repository) : BaseViewModel() {

    /** [uiState] backing property. */
    private val _uiState = MutableStateFlow<ExplorePageState>(ExplorePageState.Loading)
    /** StateFlow of UI states. States are presented by [ExplorePageState]. */
    val uiState = _uiState.asStateFlow()

    /** [event] backing property. */
    private val _event = Channel<ExplorePageEvent>(Channel.BUFFERED)
    /** Flow of events. Events are presented by [ExplorePageEvent]. */
    val event = _event.receiveAsFlow()

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
    sealed class ExplorePageEvent
}