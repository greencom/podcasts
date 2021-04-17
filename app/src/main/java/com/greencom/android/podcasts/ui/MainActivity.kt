package com.greencom.android.podcasts.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.slider.Slider
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.ActivityMainBinding
import com.greencom.android.podcasts.ui.activity.ActivityFragment
import com.greencom.android.podcasts.ui.explore.ExploreFragment
import com.greencom.android.podcasts.ui.home.HomeFragment
import com.greencom.android.podcasts.utils.OnSwipeListener
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity is the entry point for the app. This is where the Navigation component,
 * bottom navigation bar, and player bottom sheet are configured.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** View binding. */
    private lateinit var binding: ActivityMainBinding

    /** [BottomSheetBehavior] plugin of the player bottom sheet. */
    private lateinit var playerBehavior: BottomSheetBehavior<FrameLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View binding setup.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Set expanded content alpha to zero.
        binding.player.expanded.root.alpha = 0f

        // Navigation component setup.
        setupNavigation()

        // Player bottom sheet behavior setup.
        playerBehavior = BottomSheetBehavior.from(binding.player.root).apply {
            setupBottomSheetBehavior(resources.getDimension(R.dimen.bottom_nav_bar_height))
        }

        // Set listeners for the player content.
        setCollapsedContentListeners()
        setExpandedContentListeners()
    }

    /** Make player closable on back pressed. */
    override fun onBackPressed() {
        if (playerBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    /** Make player closable on outside click. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN &&
                playerBehavior.state != BottomSheetBehavior.STATE_COLLAPSED) {

            // Create a new empty Rect with [0,0,0,0] values for [left,top,right,bottom].
            val outRect = Rect()
            // Assign to the outRect the values corresponding to the coordinates of the player
            // bottom sheet within the global coordinate system starting at the left top corner
            // (corresponding to the screen resolution).
            // For example, for Galaxy S7 the values of player's Rect are [0,172,1080,1920]
            // for [left,top,right,bottom].
            binding.player.root.getGlobalVisibleRect(outRect)

            // If user touches the screen outside the player, close it.
            // Return true, which means the event was handled.
            if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                return true
            }
        }
        // Return super.dispatchTouchEvent() to handle default behavior.
        return super.dispatchTouchEvent(ev)
    }

    /** Navigation component setup. */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // Use NavHostFragment.navController instead of findNavController() for now
        // because of FragmentContainerView bug.
        val navController = navHostFragment.navController
        binding.bottomNavBar.apply {
            // Associate the bottom nav bar items with navigation graph actions.
            setupWithNavController(navController)
            // Handle Navigation behavior when the bottom navigation item is reselected.
            setupOnBottomItemReselectedBehavior(navHostFragment, navController)
        }
    }

    /**
     * Handle behavior when the bottom navigation item is reselected.
     *
     * Check if the current fragment is the starting one. If not, navigate
     * to the starting one. Otherwise, prevent fragment reloading.
     *
     * The starting fragments are fragments associated with bottom navigation
     * items (tabs).
     */
    private fun BottomNavigationView.setupOnBottomItemReselectedBehavior(
        navHostFragment: NavHostFragment,
        navController: NavController,
    ) {
        setOnNavigationItemReselectedListener {
            val currentFragment = navHostFragment.childFragmentManager.fragments[0]
            val isNavigatingNeeded = !currentFragment.isStarting()
            if (isNavigatingNeeded) {
                navController.navigateToStartingFragment(it.title)
            }
        }
    }

    /**
     * Set up and add the callback to the player [BottomSheetBehavior] to control
     * the player UI behavior when [BottomSheetBehavior] state and slideOffset change.
     *
     * @param bottomNavBarHeight the height of the bottom navigation bar in `px`.
     *                           Used to calculate the distance the bottom navigation bar
     *                           needs to be displaced.
     */
    private fun BottomSheetBehavior<FrameLayout>
            .setupBottomSheetBehavior(bottomNavBarHeight: Float) {

        val callback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                controlPlayerOnBottomSheetStateChanged(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                controlPlayerOnBottomSheetSlide(slideOffset, bottomNavBarHeight)
            }
        }
        addBottomSheetCallback(callback)
    }

    /**
     * Control the state of the player content when the player [BottomSheetBehavior]
     * state change.
     */
    private fun controlPlayerOnBottomSheetStateChanged(newState: Int) {
        when (newState) {
            // Disable the player expanded content when the player is collapsed.
            BottomSheetBehavior.STATE_COLLAPSED -> {
                binding.player.collapsed.root.isEnabled = true
                binding.player.collapsed.playPause.isClickable = true
                binding.player.expanded.cover.isClickable = false
            }
            // Disable the player collapsed content when the player is expanded.
            BottomSheetBehavior.STATE_EXPANDED -> {
                binding.player.collapsed.root.isEnabled = false
                binding.player.collapsed.playPause.isClickable = false
                binding.player.expanded.cover.isClickable = true
            }
        }
    }

    /**
     * Animate the player content and background shadows when the player
     * [BottomSheetBehavior] slide offset change.
     */
    private fun controlPlayerOnBottomSheetSlide(
            slideOffset: Float,
            bottomNavBarHeight: Float
    ) {
        // Animate alpha of the player collapsed content.
        binding.player.collapsed.root.alpha = 1f - slideOffset * 10
        binding.player.collapsed.progressBar.alpha = 1f - slideOffset * 100

        // Animate alpha of the player expanded content.
        binding.player.expanded.root.alpha = (slideOffset * 1.5f) - 0.15f

        // Animate the displacement of the bottomNavBar along the y-axis.
        binding.bottomNavBar.translationY = slideOffset * bottomNavBarHeight * 10

        // Animate player shadow.
        binding.playerShadowExternal.alpha = 1f - slideOffset * 3
        binding.playerShadowInternal.alpha = 1f - slideOffset * 3

        // Animate alpha of the background behind player.
        binding.background.alpha = slideOffset
    }

    /** Set listeners for the collapsed content of the player. */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCollapsedContentListeners() {

        // Expand the player on the frame click.
        binding.player.collapsed.root.setOnClickListener {
            expandPlayer()
        }
        // Expand the player on frame swipe.
        binding.player.collapsed.root.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeUp() {
                expandPlayer()
            }
        })
        // Expand the player on play/pause button swipe.
        binding.player.collapsed.playPause.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeUp() {
                expandPlayer()
            }
        })

        binding.player.collapsed.playPause.setOnClickListener {

        }
    }

    /** Set listeners for the expanded content of the player. */
    private fun setExpandedContentListeners() {

        binding.player.expanded.slider.addOnChangeListener { _, _, _ ->

        }

        // OnSliderTouchListener is used for animating slider thumb radius.
        val thumbRadiusDefault = resources.getDimensionPixelSize(R.dimen.slider_thumb_default)
        val thumbRadiusIncreased = resources.getDimensionPixelSize(R.dimen.slider_thumb_increased)
        val onTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                val increaseThumb = ObjectAnimator.ofInt(
                        binding.player.expanded.slider,
                        "thumbRadius",
                        slider.thumbRadius,
                        thumbRadiusIncreased
                ).apply {
                    duration = 120
                    setAutoCancel(true)
                }
                increaseThumb.start()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                val decreaseThumb = ObjectAnimator.ofInt(
                        binding.player.expanded.slider,
                        "thumbRadius",
                        slider.thumbRadius,
                        thumbRadiusDefault
                ).apply {
                    duration = 120
                    setAutoCancel(true)
                }
                decreaseThumb.start()
            }
        }
        binding.player.expanded.slider.addOnSliderTouchListener(onTouchListener)

        // The expanded content of the player is not disabled at application start
        // (because of bug?), so prevent random click on the invisible podcast cover
        // by checking the state of player bottom sheet. If player is collapsed, expand it.
        binding.player.expanded.cover.setOnClickListener {
            if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {

            }
        }

        binding.player.expanded.playPause.setOnClickListener {

        }
        binding.player.expanded.backward.setOnClickListener {

        }
        binding.player.expanded.forward.setOnClickListener {

        }
        binding.player.expanded.title.setOnClickListener {

        }
        binding.player.expanded.title.setOnClickListener {

        }
        binding.player.expanded.publisher.setOnClickListener {

        }
        binding.player.expanded.speed.setOnClickListener {

        }
        binding.player.expanded.options.setOnClickListener {

        }
    }

    /** Expand the player if it is collapsed. */
    private fun expandPlayer() {
        if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    /**
     * Return `true` if the fragment is the starting one. Otherwise return `false`.
     *
     * The starting fragments are fragments associated with bottom navigation
     * items (tabs).
     */
    private fun Fragment.isStarting(): Boolean {
        return when (this) {
            is HomeFragment -> true
            is ExploreFragment -> true
            is ActivityFragment -> true
            else -> false
        }
    }

    /**
     * Navigate to the starting fragment associated with the reselected bottom
     * navigation item.
     *
     * @param title title of the reselected bottom navigation item (tab).
     */
    private fun NavController.navigateToStartingFragment(title: CharSequence) {
        when (title) {
            resources.getString(R.string.bottom_nav_home) -> navigate(R.id.action_global_homeFragment)
            resources.getString(R.string.bottom_nav_explore) -> navigate(R.id.action_global_exploreFragment)
            resources.getString(R.string.bottom_nav_activity) -> navigate(R.id.action_global_activityFragment)
        }
    }
}