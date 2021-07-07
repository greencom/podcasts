package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExploreBinding
import com.greencom.android.podcasts.utils.AppBarLayoutStateChangeListener
import com.greencom.android.podcasts.utils.setAppBarLayoutCanDrag
import dagger.hilt.android.AndroidEntryPoint

// Saving instance state.
private const val SAVED_STATE_IS_APP_BAR_EXPANDED = "IS_APP_BAR_EXPANDED"

/**
 * Contains lists of the best podcasts for different genres implemented as tabs for
 * the TabLayout.
 */
@AndroidEntryPoint
class ExploreFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    /** Whether the app bar is collapsed or not. */
    var isAppBarExpanded = true
        private set

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        // Restore instance state.
        savedInstanceState?.apply {
            binding.appBarLayout.setExpanded(getBoolean(SAVED_STATE_IS_APP_BAR_EXPANDED), false)
        }

        initAppBar()
        initTabLayout()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putBoolean(SAVED_STATE_IS_APP_BAR_EXPANDED, isAppBarExpanded)
        }
    }

    /** App bar setup. */
    private fun initAppBar() {
        // Disable AppBarLayout dragging behavior.
        setAppBarLayoutCanDrag(binding.appBarLayout, false)

        // Track app bar state.
        binding.appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, newState: Int) {
                when (newState) {
                    EXPANDED -> isAppBarExpanded = true
                    COLLAPSED -> isAppBarExpanded = false
                    else -> {  }
                }
            }
        })
    }

    /** TabLayout and ViewPager2 setup. */
    private fun initTabLayout() {
        val pagerAdapter = ExploreViewPagerAdapter(this)
        binding.pager.adapter = pagerAdapter

        // Set up the tabs.
        val tabs = resources.getStringArray(R.array.explore_tabs)
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = tabs[position]
        }.attach()

        // Handle TabLayout onTabSelected cases.
        val onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {  }

            override fun onTabUnselected(tab: TabLayout.Tab?) {  }

            // Pass information about tab reselection to the certain ExplorePageFragment instance
            // depending on genreId.
            override fun onTabReselected(tab: TabLayout.Tab?) {
                val tabIndex = binding.tabLayout.selectedTabPosition
                val genreId = ExploreTabGenre.values()[tabIndex].id
                childFragmentManager.setFragmentResult(
                    "${ExplorePageFragment.ON_TAB_RESELECTED}$genreId",
                    Bundle()
                )
                // Expand the app bar.
                binding.appBarLayout.setExpanded(true, true)
            }
        }
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener)
    }
}