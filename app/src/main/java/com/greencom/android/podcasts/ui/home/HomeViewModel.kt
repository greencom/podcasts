package com.greencom.android.podcasts.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel used by [HomeFragment]. */
@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

    // TODO: Test code.
    fun deleteAll() = viewModelScope.launch { repository.deleteAll() }

    // TODO: Test code.
    fun deleteEpisodes() = viewModelScope.launch { repository.deleteEpisodes() }
}