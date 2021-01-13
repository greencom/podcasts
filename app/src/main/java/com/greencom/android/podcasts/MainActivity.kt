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
private const val NEVER_USED = "NEVER_USED"

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
        bottomNavBar.setupOnBottomItemReselectedBehavior(navHostFragment, navController)
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
