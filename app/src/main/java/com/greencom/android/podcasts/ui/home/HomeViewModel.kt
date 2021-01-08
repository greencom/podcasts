package com.greencom.android.podcasts.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _number = MutableLiveData(0)
    val number: LiveData<Int> get() = _number

    fun add() {
        var x = _number.value!!
        x++
        _number.value = x
    }
}