package com.greencom.android.podcasts.ui.podcast

import androidx.lifecycle.ViewModel
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(private val repository: Repository) : ViewModel()