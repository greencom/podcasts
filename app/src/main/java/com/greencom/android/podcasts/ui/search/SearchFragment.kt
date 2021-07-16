package com.greencom.android.podcasts.ui.search

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialContainerTransform
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentSearchBinding
import com.greencom.android.podcasts.utils.setupMaterialFadeThroughTransitions

// TODO
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaterialFadeThroughTransitions(
            enter = true,
            exit = true,
            popEnter = true,
            popExit = true,
        )
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBarBack.setOnClickListener { findNavController().navigateUp() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}