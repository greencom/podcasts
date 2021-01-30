package com.greencom.android.podcasts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greencom.android.podcasts.databinding.FragmentExploreSecondaryTabBinding

class ExploreSecondaryTabFragment : Fragment() {

    /** Nullable ExploreSecondaryTabFragment View binding. Use [binding] instead. */
    private var _binding: FragmentExploreSecondaryTabBinding? = null
    /** Non-null ExploreSecondaryTabFragment View binding. */
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        /** ExploreSecondaryTabFragment View binding setup. */
        _binding = FragmentExploreSecondaryTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear ExploreSecondaryTabFragment View binding.
        _binding = null
    }
}