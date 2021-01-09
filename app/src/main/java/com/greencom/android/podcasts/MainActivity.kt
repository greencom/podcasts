package com.greencom.android.podcasts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.greencom.android.podcasts.databinding.ActivityMainBinding

// Tags for navigation tabs
private const val HOME_TAB = "HomeFragment"
private const val EXPLORE_TAB = "ExploreFragment"
private const val PROFILE_TAB = "ProfileFragment"
private const val NO_TAB = "NEVER_USED"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
        handleBehaviorOnBottomItemReselected(navHostFragment, navController, bottomNavBar)
    }

    /**
     * Prevent start fragment reloading when the corresponding bottom
     * navigation item is reselected.
     */
    private fun handleBehaviorOnBottomItemReselected(
            navHostFragment: NavHostFragment,
            navController: NavController,
            bottomNavBar: BottomNavigationView,
    ) {
        bottomNavBar.setOnNavigationItemReselectedListener {
            val currentFragment = navHostFragment.childFragmentManager.fragments[0].toString()
            val currentTab = getTab(it.title)
            val isReloadingNeeded = !currentFragment.contains(currentTab)

            if (isReloadingNeeded) {
                navigateToTab(currentTab, navController)
            }
            // TODO: Implement custom behavior
        }
    }

    /**
     * Return name of the start fragment associated with the selected tab.
     */
    private fun getTab(title: CharSequence): String {
        return when (title) {
            this.getString(R.string.home) -> HOME_TAB
            this.getString(R.string.explore) -> EXPLORE_TAB
            this.getString(R.string.profile) -> PROFILE_TAB
            else -> NO_TAB
        }
    }

    /**
     * Navigate to start fragment depending on the selected tab.
     */
    private fun navigateToTab(tab: String, navController: NavController) {
        when (tab) {
            HOME_TAB -> navController.navigate(R.id.action_global_homeFragment)
            EXPLORE_TAB -> navController.navigate(R.id.action_global_exploreFragment)
            PROFILE_TAB -> navController.navigate(R.id.action_global_profileFragment)
        }
    }
}