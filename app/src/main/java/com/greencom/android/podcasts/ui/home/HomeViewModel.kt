package com.greencom.android.podcasts.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** TODO: Documentation */
@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    /** Fetch genres from ListenAPI and insert them into the `genres` table. */
    fun updateGenres() = viewModelScope.launch {
        repository.updateGenres()
    }
}
