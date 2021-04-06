package com.greencom.android.podcasts.ui.podcast

import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(private val repository: Repository) : BaseViewModel()