package com.greencom.android.podcasts.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ViewModel used by [ExploreFragment] and instances of [ExplorePrimaryPageFragment]
 * and [ExploreSecondaryPageFragment].
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    // In-memory cached best podcasts PagingData.
    private var newsCache: Flow<PagingData<Podcast>>? = null
    private var societyCache: Flow<PagingData<Podcast>>? = null
    private var educationCache: Flow<PagingData<Podcast>>? = null
    private var scienceCache: Flow<PagingData<Podcast>>? = null
    private var technologyCache: Flow<PagingData<Podcast>>? = null
    private var businessCache: Flow<PagingData<Podcast>>? = null
    private var historyCache: Flow<PagingData<Podcast>>? = null
    private var artsCache: Flow<PagingData<Podcast>>? = null
    private var sportsCache: Flow<PagingData<Podcast>>? = null
    private var healthCache: Flow<PagingData<Podcast>>? = null

    /** TODO: Documentation */
    fun getBestPodcasts(genreId: Int): Flow<PagingData<Podcast>> {
        val lastResult = when (genreId) {
            99 -> newsCache
            122 -> societyCache
            111 -> educationCache
            107 -> scienceCache
            127 -> technologyCache
            93 -> businessCache
            125 -> historyCache
            100 -> artsCache
            77 -> sportsCache
            88 -> healthCache
            else -> null
        }
        if (lastResult != null) {
            return lastResult
        }

        val result: Flow<PagingData<Podcast>> = repository.getBestPodcasts(genreId)
            .cachedIn(viewModelScope)
        when (genreId) {
            99 -> newsCache = result
            122 -> societyCache = result
            111 -> educationCache = result
            107 -> scienceCache = result
            127 -> technologyCache = result
            93 -> businessCache = result
            125 -> historyCache = result
            100 -> artsCache = result
            77 -> sportsCache = result
            88 -> healthCache = result
        }
        return result
    }
}