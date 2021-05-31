package com.greencom.android.podcasts.ui.home

import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository) : BaseViewModel() {

    // TODO: Test code.
    fun deleteEpisodes() = viewModelScope.launch { repository.deleteEpisodes() }
}