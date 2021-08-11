package com.greencom.android.podcasts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel used by [SettingsFragment]. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: Repository,
) : ViewModel() {

    /** Set app theme mode. */
    fun setTheme(mode: Int) = viewModelScope.launch {
        repository.setTheme(mode)
    }

    /** Get a Flow with an app theme mode. */
    fun getTheme(): Flow<Int?> = repository.getTheme()

    /** Save subscription presentation mode. */
    fun setSubscriptionMode(mode: Int) = viewModelScope.launch {
        repository.setSubscriptionMode(mode)
    }

    /** Get a Flow with a subscription presentation mode. */
    fun getSubscriptionMode(): Flow<Int?> = repository.getSubscriptionMode()

    /** Remove all episodes from the database. */
    fun deleteEpisodes() = viewModelScope.launch {
        repository.deleteEpisodes()
    }

    /** Clear the whole database. */
    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }
}