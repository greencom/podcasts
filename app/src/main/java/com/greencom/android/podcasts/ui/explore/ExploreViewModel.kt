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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel used by [ExploreFragment] and instances of [ExplorePrimaryPageFragment]
 * and [ExploreSecondaryPageFragment].
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    /** Backing property for [navigateToPodcast]. */
    private val _navigateToPodcast = MutableLiveData<Event<String>>()
    /** LiveData<Event<String>> that contains the podcast ID to navigate to. */
    val navigateToPodcast: LiveData<Event<String>> get() = _navigateToPodcast

    /** Backing property for [message]. Use [setMessage] to set a new value. */
    private val _message = MutableLiveData<Event<Int>>()
    /**
     * LiveData<Event<Int>> that contains a string resource for a message
     * to show by toast or snackbar.
     */
    val message: LiveData<Event<Int>> get() = _message

    /** Flow with a list of the best podcasts for a given genre ID. */
    @ExperimentalCoroutinesApi
    fun getBestPodcasts(genreId: Int): Flow<State> = repository.getBestPodcastsFlow(genreId)

    /** Update the best podcasts for a given genre ID. */
    // Consider using Dispatchers.IO to get rid of the freeze on first swipe-to-refresh?
    fun updateBestPodcasts(genreId: Int) {
        viewModelScope.launch {
            when (repository.updateBestPodcasts(genreId)) {
                is State.Success<*> -> {
                    setMessage(R.string.explore_podcasts_updated)
                }
                is State.Error -> {
                    setMessage(R.string.explore_something_went_wrong)
                }
                // Get rid of Lint warnings.
                else -> {  }
            }
        }
    }

    /**
     * Fetch the best podcasts for a given genre ID from ListenAPI and insert them into
     * the database.
     */
    fun fetchBestPodcasts(genreId: Int) {
        viewModelScope.launch {
            if (repository.fetchBestPodcasts(genreId) is State.Error) {
                setMessage(R.string.explore_something_went_wrong)
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

    /** Navigate to the podcast page. */
    fun onPodcastClick(podcast: Podcast) {
        _navigateToPodcast.value = Event(podcast.id)
    }
}