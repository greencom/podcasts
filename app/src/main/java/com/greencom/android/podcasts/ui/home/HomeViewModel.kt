package com.greencom.android.podcasts.ui.home

import androidx.lifecycle.ViewModel
import com.greencom.android.podcasts.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: Repository) : ViewModel() {

}