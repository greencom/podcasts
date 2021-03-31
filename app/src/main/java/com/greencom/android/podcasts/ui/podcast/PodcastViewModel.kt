package com.greencom.android.podcasts.ui.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO
@HiltViewModel
class PodcastViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    // TODO
    private val _podcast = MutableStateFlow<State>(State.Loading)
    // TODO
    val podcast = _podcast.asStateFlow()

    // TODO
    fun getPodcast(id: String) = viewModelScope.launch {
        _podcast.value = repository.getPodcast(id)
    }

    /** Update the subscription on a podcast with a given value. */
    fun updateSubscription(podcastId: String, subscribed: Boolean) = viewModelScope.launch {
        repository.updateSubscription(podcastId, subscribed)
    }
}