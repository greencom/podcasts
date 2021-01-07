package com.greencom.android.podcasts.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.greencom.android.podcasts.R

class HomeFragment : Fragment() {

//    private val viewModel: HomeViewModel by navGraphViewModels(R.id.nav_graph)
//    private val viewModel: HomeViewModel by activityViewModels()
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
}