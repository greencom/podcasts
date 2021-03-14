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

    // In-memory cached the best podcasts by genre.
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

    // Show whether any item on the appropriate list has changed.
    private var newsChanged = false
    private var societyChanged = false
    private var educationChanged = false
    private var scienceChanged = false
    private var technologyChanged = false
    private var businessChanged = false
    private var historyChanged = false
    private var artsChanged = false
    private var sportsChanged = false
    private var healthChanged = false

    /** Get a list of the best podcasts for a given genre ID. */
    fun getBestPodcasts(genreId: Int): StateFlow<State> {
        // Get from in-memory cache, if the appropriate podcast list has not changed.
        if (bestPodcastsHaveNotChanged(genreId)) {
            val lastResult = getBestPodcastsFromCache(genreId).asStateFlow()
            if (lastResult.value is State.Success<*>) {
                return lastResult
            }
        }

        // Get from repository (database or network).
        // Set State.Loading.
        cacheBestPodcasts(genreId, State.Loading)
        viewModelScope.launch {
            repository.getBestPodcasts(genreId, getBestPodcastsFromCache(genreId))
        }
        // Reset `*bestPodcasts*Changed` value.
        setBestPodcastsHaveChanged(genreId, false)
        return getBestPodcastsFromCache(genreId).asStateFlow()
    }

    /** Update the subscription on a given podcast. */
    fun updateSubscription(podcast: Podcast, newValue: Boolean) = viewModelScope.launch {
        repository.updateSubscription(podcast, newValue)
        setBestPodcastsHaveChanged(podcast.inBestForGenre, true)
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
        } ?: MutableStateFlow(State.Error(IllegalArgumentException("Wrong genre ID.")))
    }

    /** Set the appropriate `*bestPodcasts*Changed` property to a given value. */
    private fun setBestPodcastsHaveChanged(genreId: Int, value: Boolean) {
        when (genreId) {
            99 -> newsChanged = value
            122 -> societyChanged = value
            111 -> educationChanged = value
            107 -> scienceChanged = value
            127 -> technologyChanged = value
            93 -> businessChanged = value
            125 -> historyChanged = value
            100 -> artsChanged = value
            77 -> sportsChanged = value
            88 -> healthChanged = value
        }
    }

    /**
     * Return `true` if the appropriate podcast list has not changed.
     * Otherwise, return `false`.
     */
    private fun bestPodcastsHaveNotChanged(genreId: Int): Boolean {
        return when (genreId) {
            99 -> !newsChanged
            122 -> !societyChanged
            111 -> !educationChanged
            107 -> !scienceChanged
            127 -> !technologyChanged
            93 -> !businessChanged
            125 -> !historyChanged
            100 -> !artsChanged
            77 -> !sportsChanged
            88 -> !healthChanged
            else -> true
        }
    }
}