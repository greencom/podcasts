package com.greencom.android.podcasts.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    private var newsHasChanged = false
    private var societyHasChanged = false
    private var educationHasChanged = false
    private var scienceHasChanged = false
    private var technologyHasChanged = false
    private var businessHasChanged = false
    private var historyHasChanged = false
    private var artsHasChanged = false
    private var sportsHasChanged = false
    private var healthHasChanged = false

    private val _toastMessage = MutableStateFlow("")
    /** `StateFlow` toast message. */
    val toastMessage = _toastMessage.asStateFlow()

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
    fun updateSubscription(podcast: Podcast, subscribed: Boolean) = viewModelScope.launch {
        repository.updateSubscription(podcast, subscribed)
        setBestPodcastsHaveChanged(podcast.genreId, true)
    }

    /**
     * Update the best podcasts for a given genre ID. Pass the `MutableStateFlow<Boolean>`
     * [isRefreshing] in addition to maintain the refreshing state of the swipe-to-refresh.
     */
    // Using Dispatchers.IO to get rid of the freeze on first swipe-to-refresh.
    fun updateBestPodcasts(genreId: Int, isRefreshing: MutableStateFlow<Boolean>) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBestPodcasts(
                genreId,
                getBestPodcastsFromCache(genreId),
                _toastMessage,
                isRefreshing
            )
        }

    /** Set [toastMessage] value to an empty string. */
    fun resetToast() {
        _toastMessage.value = ""
    }

    /** Cache the best podcasts based on the genre ID with a given [State]. */
    private fun cacheBestPodcasts(genreId: Int, state: State) {
        when (genreId) {
            ExploreTabGenre.NEWS.id -> newsCache.value = state
            ExploreTabGenre.SOCIETY_AND_CULTURE.id -> societyCache.value = state
            ExploreTabGenre.EDUCATION.id -> educationCache.value = state
            ExploreTabGenre.SCIENCE.id -> scienceCache.value = state
            ExploreTabGenre.TECHNOLOGY.id -> technologyCache.value = state
            ExploreTabGenre.BUSINESS.id -> businessCache.value = state
            ExploreTabGenre.HISTORY.id -> historyCache.value = state
            ExploreTabGenre.ARTS.id -> artsCache.value = state
            ExploreTabGenre.SPORTS.id -> sportsCache.value = state
            ExploreTabGenre.HEALTH_AND_FITNESS.id -> healthCache.value = state
        }
    }

    /** Get the best podcasts from the in-memory cache for a given genre ID. */
    private fun getBestPodcastsFromCache(genreId: Int): MutableStateFlow<State> {
        return when (genreId) {
            ExploreTabGenre.NEWS.id -> newsCache
            ExploreTabGenre.SOCIETY_AND_CULTURE.id -> societyCache
            ExploreTabGenre.EDUCATION.id -> educationCache
            ExploreTabGenre.SCIENCE.id -> scienceCache
            ExploreTabGenre.TECHNOLOGY.id -> technologyCache
            ExploreTabGenre.BUSINESS.id -> businessCache
            ExploreTabGenre.HISTORY.id -> historyCache
            ExploreTabGenre.ARTS.id -> artsCache
            ExploreTabGenre.SPORTS.id -> sportsCache
            ExploreTabGenre.HEALTH_AND_FITNESS.id -> healthCache
            else -> null
        } ?: MutableStateFlow(
            State.Error(IllegalArgumentException("No information about the given genre ID")))
    }

    /** Set the corresponding `*bestPodcasts*Changed` property to a given value. */
    private fun setBestPodcastsHaveChanged(genreId: Int, value: Boolean) {
        when (genreId) {
            ExploreTabGenre.NEWS.id -> newsHasChanged = value
            ExploreTabGenre.SOCIETY_AND_CULTURE.id -> societyHasChanged = value
            ExploreTabGenre.EDUCATION.id -> educationHasChanged = value
            ExploreTabGenre.SCIENCE.id -> scienceHasChanged = value
            ExploreTabGenre.TECHNOLOGY.id -> technologyHasChanged = value
            ExploreTabGenre.BUSINESS.id -> businessHasChanged = value
            ExploreTabGenre.HISTORY.id -> historyHasChanged = value
            ExploreTabGenre.ARTS.id -> artsHasChanged = value
            ExploreTabGenre.SPORTS.id -> sportsHasChanged = value
            ExploreTabGenre.HEALTH_AND_FITNESS.id -> healthHasChanged = value
        }
    }

    /**
     * Return `true` if the corresponding podcast list has not changed.
     * Otherwise, return `false`.
     */
    private fun bestPodcastsHaveNotChanged(genreId: Int): Boolean {
        return when (genreId) {
            ExploreTabGenre.NEWS.id -> !newsHasChanged
            ExploreTabGenre.SOCIETY_AND_CULTURE.id -> !societyHasChanged
            ExploreTabGenre.EDUCATION.id -> !educationHasChanged
            ExploreTabGenre.SCIENCE.id -> !scienceHasChanged
            ExploreTabGenre.TECHNOLOGY.id -> !technologyHasChanged
            ExploreTabGenre.BUSINESS.id -> !businessHasChanged
            ExploreTabGenre.HISTORY.id -> !historyHasChanged
            ExploreTabGenre.ARTS.id -> !artsHasChanged
            ExploreTabGenre.SPORTS.id -> !sportsHasChanged
            ExploreTabGenre.HEALTH_AND_FITNESS.id -> !healthHasChanged
            else -> true
        }
    }
}