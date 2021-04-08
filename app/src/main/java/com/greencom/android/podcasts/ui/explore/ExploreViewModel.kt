package com.greencom.android.podcasts.ui.explore

import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.BaseViewModel
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel used by [ExploreFragment] and instances of [ExplorePrimaryPageFragment]
 * and [ExploreSecondaryPageFragment].
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: Repository) : BaseViewModel() {

    // TODO
    private val _event = MutableStateFlow<ExploreEvent>(ExploreEvent.Empty)
    val event = _event.asStateFlow()

    // TODO
    fun getBestPodcasts(genreId: Int): Flow<State> = repository.getBestPodcasts(genreId)

    // TODO
    sealed class ExploreEvent {
        object Empty : ExploreEvent()
    }
}