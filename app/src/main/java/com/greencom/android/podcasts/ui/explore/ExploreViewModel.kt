package com.greencom.android.podcasts.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel used by [ExploreFragment] and instances of [ExplorePrimaryPageFragment]
 * and [ExploreSecondaryPageFragment].
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    // In-memory cached best podcasts by genre.
    private val newsCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val societyCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val educationCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val scienceCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val technologyCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val businessCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val historyCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val artsCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val sportsCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }
    private val healthCache: MutableStateFlow<State> by lazy { MutableStateFlow(State.Init) }

    /** Get a list of the best podcasts for a given genre ID. */
    fun getBestPodcasts(genreId: Int): StateFlow<State> {
        // Get from in-memory cache.
        val lastResult = getBestPodcastsFromCache(genreId).asStateFlow()
        if (lastResult.value is State.Success<*>) return lastResult

        // Set State.Loading.
        cacheBestPodcasts(genreId, State.Loading)
        // Get from repository (database or network).
        viewModelScope.launch {
            repository.getBestPodcasts(genreId, getBestPodcastsFromCache(genreId))
        }
        return getBestPodcastsFromCache(genreId).asStateFlow()
    }

    /** Update the subscription for a given podcast. */
    fun updateSubscription(podcast: Podcast) = viewModelScope.launch {
        repository.updatePodcastSubscription(podcast)
    }

    /** Cache the best podcasts based on the genre ID with a given [State]. */
    private fun cacheBestPodcasts(genreId: Int, state: State) {
        when (genreId) {
            99 -> newsCache.value = state
            122 -> societyCache.value = state
            111 -> educationCache.value = state
            107 -> scienceCache.value = state
            127 -> technologyCache.value = state
            93 -> businessCache.value = state
            125 -> historyCache.value = state
            100 -> artsCache.value = state
            77 -> sportsCache.value = state
            88 -> healthCache.value = state
        }
    }

    /** Get the best podcasts from the in-memory cache for a given genre ID. */
    private fun getBestPodcastsFromCache(genreId: Int): MutableStateFlow<State> {
        return when (genreId) {
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
        } ?: MutableStateFlow(State.Error(IllegalArgumentException("Wrong genre ID")))
    }

    /** Invalidate best podcasts cache for a given genre ID. */
    private fun invalidateBestPodcastsCache(genreId: Int) {
        when (genreId) {
            99 -> newsCache.value = State.Init
            122 -> societyCache.value = State.Init
            111 -> educationCache.value = State.Init
            107 -> scienceCache.value = State.Init
            127 -> technologyCache.value = State.Init
            93 -> businessCache.value = State.Init
            125 -> historyCache.value = State.Init
            100 -> artsCache.value = State.Init
            77 -> sportsCache.value = State.Init
            88 -> healthCache.value = State.Init
        }
    }
}