package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExploreBinding
import com.greencom.android.podcasts.utils.AppBarLayoutStateChangeListener
import com.greencom.android.podcasts.utils.setAppBarLayoutCanDrag
import com.greencom.android.podcasts.utils.extensions.setupMaterialFadeThroughTransitions
import com.greencom.android.podcasts.utils.extensions.setupMaterialSharedAxisTransitions
import dagger.hilt.android.AndroidEntryPoint

// Saving instance state.
private const val SAVED_STATE_IS_APP_BAR_EXPANDED = "IS_APP_BAR_EXPANDED"

/**
 * Contains lists of the best podcasts for different genres implemented as tabs for
 * the TabLayout and ViewPager2.
 */
@AndroidEntryPoint
class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    /** Whether the app bar is expanded or not. */
    var isAppBarExpanded = true
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaterialSharedAxisTransitions()
    }

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
        // Navigate to SearchFragment.
        binding.searchButton.setOnClickListener {
            val direction = ExploreFragmentDirections.actionExploreFragmentToSearchFragment()
            val transitionName = getString(R.string.search_transition_name)
            val extras = FragmentNavigatorExtras(binding.searchButton to transitionName)
            // Set the appropriate transition animations.
            setupMaterialFadeThroughTransitions(
                exit = true,
                popEnter = true,
            )
            findNavController().navigate(direction, extras)
        }

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
        binding.pager.adapter = ExploreViewPagerAdapter(this)

        // Tabs setup.
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
                    ExplorePageFragment.createOnTabReselectedKey(genreId),
                    Bundle()
                )
                // Expand the app bar.
                binding.appBarLayout.setExpanded(true, true)
            }
        }
        binding.tabLayout.addOnTabSelectedListener(onTabSelectedListener)
    }
}