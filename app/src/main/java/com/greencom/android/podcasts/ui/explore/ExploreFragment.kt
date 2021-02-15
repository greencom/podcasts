package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExploreBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Contains lists of the best podcasts for different genres implemented as tabs for
 * the TabLayout and provides a way for searching.
 */
@AndroidEntryPoint
class ExploreFragment : Fragment() {

    /** Nullable View binding. Use [binding] instead. */
    private var _binding: FragmentExploreBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /** View binding setup. */
        _binding = FragmentExploreBinding.inflate(inflater, container, false)

        /** TabLayout and ViewPager2 setup. */
        val pagerAdapter = ExplorePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        // Bind the TabLayout with the ViewPager2.
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            when (position) {
                0 -> tab.text = resources.getString(R.string.all)
                1 -> tab.text = resources.getString(R.string.news)
                2 -> tab.text = resources.getString(R.string.society_culture)
                3 -> tab.text = resources.getString(R.string.education)
                4 -> tab.text = resources.getString(R.string.science)
                5 -> tab.text = resources.getString(R.string.technology)
                6 -> tab.text = resources.getString(R.string.business)
                7 -> tab.text = resources.getString(R.string.history)
                8 -> tab.text = resources.getString(R.string.arts)
                9 -> tab.text = resources.getString(R.string.sports)
                10 -> tab.text = resources.getString(R.string.health_fitness)
            }
        }.attach()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clear View binding.
        _binding = null
    }
}
