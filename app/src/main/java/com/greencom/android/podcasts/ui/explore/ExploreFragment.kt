package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExploreBinding
import com.greencom.android.podcasts.utils.AppBarLayoutStateChangeListener
import dagger.hilt.android.AndroidEntryPoint

/**
 * Contains lists of the best podcasts for different genres implemented as tabs for
 * the TabLayout.
 */
@AndroidEntryPoint
class ExploreFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentExploreBinding? = null
    /** Non-null View binding. */
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
            binding.appBarLayout.setExpanded(getBoolean(STATE_IS_APP_BAR_EXPANDED), false)
        }

        setupAppBar()
        setupTabLayout()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putBoolean(STATE_IS_APP_BAR_EXPANDED, isAppBarExpanded)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** App bar setup. */
    private fun setupAppBar() {
        // Disable AppBarLayout dragging behavior.
        if (binding.appBarLayout.layoutParams != null) {
            val appBarParams = binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            val appBarBehavior = AppBarLayout.Behavior()
            appBarBehavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                    return false
                }
            })
            appBarParams.behavior = appBarBehavior
        }

        // Track app bar state.
        binding.appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, newState: AppBarLayoutState) {
                when (newState) {
                    AppBarLayoutState.EXPANDED -> isAppBarExpanded = true
                    AppBarLayoutState.COLLAPSED -> isAppBarExpanded = false
                    else -> {  }
                }
            }
        })
    }

    /** TabLayout and ViewPager2 setup. */
    private fun setupTabLayout() {
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

    companion object {
        // Saving instance state.
        private const val STATE_IS_APP_BAR_EXPANDED = "state_is_app_bar_expanded"
    }
}