package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greencom.android.podcasts.databinding.FragmentExplorePrimaryPageBinding

/**
 * Represents the first page of the ViewPager2 in the ExploreFragment.
 * The first page contains the podcast lists for different genres.
 */
class ExplorePrimaryPageFragment : Fragment() {

    /** Nullable View binding. Use [binding] instead. */
    private var _binding: FragmentExplorePrimaryPageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /** View binding setup. */
        _binding = FragmentExplorePrimaryPageBinding.inflate(inflater, container, false)

        binding.textView.text = "All"

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear View binding.
        _binding = null
    }
}
