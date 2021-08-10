package com.greencom.android.podcasts.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentActivityBinding
import com.greencom.android.podcasts.ui.activity.bookmarks.ActivityBookmarksFragment
import com.greencom.android.podcasts.ui.activity.history.ActivityHistoryFragment
import com.greencom.android.podcasts.ui.activity.inprogress.ActivityInProgressFragment
import com.greencom.android.podcasts.utils.extensions.setupMaterialSharedAxisTransitions
import dagger.hilt.android.AndroidEntryPoint

/** Contains a bookmarks list and a listening history made with TabLayout and ViewPager2. */
@AndroidEntryPoint
class ActivityFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaterialSharedAxisTransitions()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        binding.root.doOnPreDraw { startPostponedEnterTransition() }

        initTabLayout()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** TabLayout and ViewPager2 setup. */
    private fun initTabLayout() {
        val tabs = resources.getStringArray(R.array.activity_tabs)

        binding.viewPager.adapter = ActivityViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = tabs.size - 1

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()

        // Scroll the appropriate list to the top on tab reselected.
        val onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {  }

            override fun onTabUnselected(tab: TabLayout.Tab?) {  }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                val key = when (binding.tabLayout.selectedTabPosition) {
                    0 -> ActivityBookmarksFragment.createOnTabReselectedKey()
                    1 -> ActivityInProgressFragment.createOnTabReselectedKey()
                    2 -> ActivityHistoryFragment.createOnTabReselectedKey()
                    else -> ""
                }
                childFragmentManager.setFragmentResult(key, Bundle())
            }
        }
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener)
    }
}