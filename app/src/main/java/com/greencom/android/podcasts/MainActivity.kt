package com.greencom.android.podcasts

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.greencom.android.podcasts.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding setup
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Bottom navigation bar setup
        // Use NavHostFragment.navController instead of findNavController()
        // because of FragmentContainerView bug
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavBar = binding.bottomNavBar
        bottomNavBar.setupWithNavController(navController)

        // TODO: Disable reloading on bottom menu item reselected
        bottomNavBar.setOnNavigationItemReselectedListener {
            Toast.makeText(this, "${it.title}", Toast.LENGTH_SHORT).show()
        }
    }

    // TODO: Disable reloading on bottom menu item reselected
    private fun disableReloadingOnBottomItemReselected(
            navHostFragment: NavHostFragment,
            navController: NavController,
            bottomNavBar: BottomNavigationView,
    ) {
        bottomNavBar.setOnNavigationItemReselectedListener {
            val x = navHostFragment.childFragmentManager.fragments[0].toString()
                    .contains("${it.title}Fragment")
            if (!x) {
                when (it.title) {
                    "Home" -> navController.navigate(R.id.action_global_homeFragment)
                    "Explore" -> navController.navigate(R.id.action_global_exploreFragment)
                    "Profile" -> navController.navigate(R.id.action_global_profileFragment)
                }
            }
        }
    }
}