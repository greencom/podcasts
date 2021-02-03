package com.greencom.android.podcasts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExploreBinding
import com.greencom.android.podcasts.viewmodels.ExploreViewModel

/** TODO: Documentation */
class ExploreFragment : Fragment() {

    /** Nullable View binding. Use [binding] instead. */
    private var _binding: FragmentExploreBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by navGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /** View binding setup. */
        _binding = FragmentExploreBinding.inflate(inflater, container, false)

        /** TabLayout and ViewPager2 setup. */
        val pagerAdapter = PagerAdapter(this)
        binding.pager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            when (position) {
                0 -> tab.text = "For You"
                1 -> tab.text = "News"
                2 -> tab.text = "Culture"
                3 -> tab.text = "Education"
                4 -> tab.text = "Business"
            }
        }.attach()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** TODO: Documentation */
    private inner class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                ExplorePrimaryPageFragment()
            } else {
                ExploreSecondaryPageFragment()
            }
        }
    }
}
