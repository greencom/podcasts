package com.greencom.android.podcasts.ui.explore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greencom.android.podcasts.network.BestPodcasts
import com.greencom.android.podcasts.network.ListenApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExploreViewModel : ViewModel() {

    private val listenApi = ListenApi.retrofitService

    private val _bestPodcasts = MutableLiveData<BestPodcasts>()
    val bestPodcasts: LiveData<BestPodcasts> get() = _bestPodcasts

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    init {
        viewModelScope.launch {
            try {
                _bestPodcasts.value = withContext(Dispatchers.IO) {
                    listenApi.getBestPodcasts()
                }
            } catch (e: Exception) {
                _error.value = "${e.message}"
            }
        }
    }
}