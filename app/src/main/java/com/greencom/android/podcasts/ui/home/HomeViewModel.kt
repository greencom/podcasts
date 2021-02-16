package com.greencom.android.podcasts.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    /** Fetch genre list from ListenAPI and insert it into the `genres` table. */
    fun loadGenres() = viewModelScope.launch {
        repository.loadGenres()
    }
}
