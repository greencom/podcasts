package com.greencom.android.podcasts.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel used by [ExploreFragment] and instances of [ExplorePrimaryPageFragment]
 * and [ExploreSecondaryPageFragment].
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    /** Represents the state of loading genres using [State] class. */
    val genresState = repository.genresState

    /** Load genre list from ListenAPI and insert it into the `genres` table. */
    fun loadGenres() = viewModelScope.launch {
        repository.loadGenres()
    }
}
