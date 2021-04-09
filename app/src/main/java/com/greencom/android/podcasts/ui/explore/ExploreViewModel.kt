package com.greencom.android.podcasts.ui.explore

import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.BaseViewModel
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ViewModel used by [ExploreFragment] and instances of [ExplorePrimaryPageFragment]
 * and [ExploreSecondaryPageFragment].
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: Repository) : BaseViewModel() {

    /**
     * Get a Flow with a list of the best podcasts for a given genre ID. The result
     * is presented as [State] object.
     */
    fun getBestPodcasts(genreId: Int): Flow<State> = repository.getBestPodcasts(genreId)
}