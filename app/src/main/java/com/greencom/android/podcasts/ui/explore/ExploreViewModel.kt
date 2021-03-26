package com.greencom.android.podcasts.ui.explore

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.Event
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

    // In-memory cached the best podcasts by genre (tab).
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

    /** Backing property for a [message]. */
    private val _message = MutableLiveData<Event<Int>>()
    /**
     * `LiveData<Event<Int>>` that contains a string resource for a message
     * to show by toast or snackbar.
     */
    val message: LiveData<Event<Int>> get() = _message

    /** Get a list of the best podcasts for a given genre ID. */
    fun getBestPodcasts(genreId: Int): StateFlow<State> {
        writeToCache(genreId, State.Loading)
        viewModelScope.launch {
            val state = repository.getBestPodcasts(genreId)
            writeToCache(genreId, state)
        }
        return getFromCache(genreId).asStateFlow()
    }

    /**
     * Update the best podcasts for a given genre ID. Returns the result as a [State] object.
     */
    // Use Dispatchers.IO to get rid of the freeze on first swipe-to-refresh.
    fun updateBestPodcasts(genreId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val state = repository.updateBestPodcasts(genreId)) {
                is State.Success<*> -> {
                    writeToCache(genreId, state)
                    setMessage(R.string.explore_podcasts_updated)
                }
                is State.Error -> {
                    setMessage(R.string.explore_something_went_wrong)
                }
            }
        }
    }

    /** Update the subscription on a podcast with a given value. */
    fun updateSubscription(podcast: Podcast, subscribed: Boolean) = viewModelScope.launch {
        repository.updateSubscription(podcast, subscribed)
    }

    /** Set a given string resource into [message]. */
    private fun setMessage(@StringRes res: Int) {
        _message.postValue(Event(res))
    }

    /**
     * Write to cache the appropriate [State] with the best podcasts based on the genre ID.
     */
    private fun writeToCache(genreId: Int, state: State) {
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

    /**
     * Get the appropriate `MutableStateFlow` of [State] with the best podcasts from the
     * cache based on the genre ID. Return MutableStateFlow<State.Error()> with
     * [IllegalArgumentException] if there is no cache for a given ID.
     *
     * @return `MutableStateFlow<State>`.
     */
    private fun getFromCache(genreId: Int): MutableStateFlow<State> {
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
            State.Error(IllegalArgumentException("There is no cache for a given ID")))
    }
}