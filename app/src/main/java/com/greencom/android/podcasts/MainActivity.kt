package com.greencom.android.podcasts

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.greencom.android.podcasts.databinding.ActivityMainBinding
import com.greencom.android.podcasts.utils.convertDpToPx

// Tags for navigation tabs
private const val HOME_TAB = "HomeFragment"
private const val EXPLORE_TAB = "ExploreFragment"
private const val PROFILE_TAB = "ProfileFragment"
private const val NEVER_USED = "NEVER_USED"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding setup
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Bottom navigation bar setup
        // - Use NavHostFragment.navController instead of findNavController()
        //   because of FragmentContainerView bug
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavBar = binding.bottomNavBar
        bottomNavBar.setupWithNavController(navController)
        // Handle behavior when the bottom navigation item is reselected
        bottomNavBar.setupOnBottomItemReselectedBehavior(navHostFragment, navController)
        // Convert bottom navigation bar from dp to px
        val bottomNavBarHeight = convertDpToPx(56)

        // TODO: Make main part of player bottom sheet invisible at start
        binding.included.contentExpanded.alpha = 0f

        // Player bottom sheet setup
        val playerBottomSheetBehavior = BottomSheetBehavior
                .from(binding.included.playerBottomSheet)
        playerBottomSheetBehavior.setupBottomSheetBehavior(bottomNavBarHeight)

        // TODO: Open player bottom sheet on click

        // TODO: Change onBackPressed() behavior to close player bottom sheet at first, if it is open
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            @SuppressLint("SwitchIntDef")
            override fun handleOnBackPressed() {
                when (playerBottomSheetBehavior.state) {
                    BottomSheetBehavior.STATE_DRAGGING ->
                        playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    BottomSheetBehavior.STATE_SETTLING ->
                        playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    BottomSheetBehavior.STATE_EXPANDED ->
                        playerBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    /**
     * TODO: Complete and write documentation
     */
    private fun BottomSheetBehavior<CardView>.setupBottomSheetBehavior(bottomNavBarHeight: Float) {
        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            @SuppressLint("SwitchIntDef")
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                updateOnBackPressedCallback(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                animatePlayerBottomSheetContentOnSlide(slideOffset, bottomNavBarHeight)
            }
        }
        addBottomSheetCallback(bottomSheetCallback)
    }

    /**
     * TODO: Complete and write documentation
     */
    private fun updateOnBackPressedCallback(bottomSheetState: Int) {
        when (bottomSheetState) {
            BottomSheetBehavior.STATE_COLLAPSED -> onBackPressedCallback.isEnabled = false
            BottomSheetBehavior.STATE_DRAGGING -> onBackPressedCallback.isEnabled = true
            BottomSheetBehavior.STATE_SETTLING -> onBackPressedCallback.isEnabled = true
            BottomSheetBehavior.STATE_EXPANDED -> onBackPressedCallback.isEnabled = true
        }
    }

    /**
     * TODO: Complete and write documentation
     */
    private fun animatePlayerBottomSheetContentOnSlide(
            slideOffset: Float,
            bottomNavBarHeight: Float
    ) {
        binding.included.contentCollapsed.alpha = 1f - slideOffset * 7
        binding.included.contentExpanded.alpha = slideOffset - 0.2f
        binding.bottomNavBar.translationY = slideOffset * bottomNavBarHeight * 7
    }

    /**
     * Handle behavior when the bottom navigation item is reselected.
     *
     * Check if the current fragment is the starting one. If not, navigate to the starting one.
     * Otherwise, prevent fragment reloading.
     */
    private fun BottomNavigationView.setupOnBottomItemReselectedBehavior(
            navHostFragment: NavHostFragment,
            navController: NavController,
    ) {
        setOnNavigationItemReselectedListener {
            val currentFragment = navHostFragment.childFragmentManager.fragments[0].toString()
            val currentTab = getCurrentTab(it.title)
            val isReloadingNeeded = !currentFragment.contains(currentTab)

            if (isReloadingNeeded) {
                navigateToTab(currentTab, navController)
            }
        }
    }

    /**
     * Return the tag of the start fragment associated with the selected tab.
     */
    private fun getCurrentTab(title: CharSequence): String {
        return when (title) {
            this.getString(R.string.home) -> HOME_TAB
            this.getString(R.string.explore) -> EXPLORE_TAB
            this.getString(R.string.profile) -> PROFILE_TAB
            else -> NEVER_USED
        }
    }

    /**
     * Navigate to the start fragment associated with the selected tab.
     */
    private fun navigateToTab(tab: String, navController: NavController) {
        when (tab) {
            HOME_TAB -> navController.navigate(R.id.action_global_homeFragment)
            EXPLORE_TAB -> navController.navigate(R.id.action_global_exploreFragment)
            PROFILE_TAB -> navController.navigate(R.id.action_global_profileFragment)
        }
    }
}
