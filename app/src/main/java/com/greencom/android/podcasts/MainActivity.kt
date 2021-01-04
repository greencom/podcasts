package com.greencom.android.podcasts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Migrate to ViewBinding
        setContentView(R.layout.activity_main)

        // Bottom navigation bar setup
        // Use NavHostFragment.navController instead of findNavController()
        // because of FragmentContainerView bug
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavBar = findViewById<BottomNavigationView>(R.id.bottom_nav_bar)
        bottomNavBar.setupWithNavController(navController)
    }
}