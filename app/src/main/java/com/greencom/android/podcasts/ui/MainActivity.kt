package com.greencom.android.podcasts.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.slider.Slider
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.ActivityMainBinding
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.ui.activity.ActivityFragment
import com.greencom.android.podcasts.ui.explore.ExploreFragment
import com.greencom.android.podcasts.ui.home.HomeFragment
import com.greencom.android.podcasts.utils.OnSwipeListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * MainActivity is the entry point for the app. This is where the Navigation component,
 * bottom navigation bar, and player bottom sheet are configured.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** View binding. */
    private lateinit var binding: ActivityMainBinding

    /** App repository. */
    @Inject lateinit var repository: Repository

    /** [BottomSheetBehavior] plugin of the player bottom sheet. */
    private lateinit var playerBehavior: BottomSheetBehavior<FrameLayout>

    init {
        /** Update the `genres` table, if it is empty. */
        lifecycleScope.launchWhenCreated {
            repository.updateGenres()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /** MainActivity View binding setup. */
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /** Navigation component setup. */
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

        /** Player [BottomSheetBehavior] setup. */
        playerBehavior = BottomSheetBehavior.from(binding.player.root).apply {
            setupBottomSheetBehavior(resources.getDimension(R.dimen.bottom_nav_bar_height))
        }

        /** Player content setup. */
        setupPlayer()
    }

    /** Make player closable on back pressed. */
    @SuppressLint("SwitchIntDef")
    override fun onBackPressed() {
        if (
                playerBehavior.state == BottomSheetBehavior.STATE_DRAGGING ||
                playerBehavior.state == BottomSheetBehavior.STATE_SETTLING ||
                playerBehavior.state == BottomSheetBehavior.STATE_EXPANDED
        ) {
            playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    /** Make player closable on outside click. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN &&
                playerBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {

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
     *        Used to calculate the distance the bottom navigation bar needs to be
     *        displaced.
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

    /** Player content setup. */
    private fun setupPlayer() {

        // Select text views to make them auto scrollable, if needed.
        binding.player.expanded.title.isSelected = true
        binding.player.expanded.publisher.isSelected = true
        binding.player.collapsed.title.isSelected = true

        // Set listeners for the player content.
        setCollapsedContentListeners()
        setExpandedContentListeners()
    }

    /**
     * Animate the player content and background shadows when the player
     * [BottomSheetBehavior] slide offset change.
     */
    private fun controlPlayerOnBottomSheetSlide(
            slideOffset: Float,
            bottomNavBarHeight: Float
    ) {
        // Animate alpha of the player collapsed content. VERIFIED.
        binding.player.collapsed.root.alpha = 1f - slideOffset * 10
        binding.player.collapsed.progressBar.alpha = 1f - slideOffset * 100

        // Animate alpha of the player expanded content. VERIFIED.
        binding.player.expanded.root.alpha = (slideOffset * 1.5f) - 0.15f

        // Animate the displacement of the bottomNavBar along the y-axis. VERIFIED.
        binding.bottomNavBar.translationY = slideOffset * bottomNavBarHeight * 10

        // Change elevations of the player and bottomNavBar to overlap the background. VERIFIED.
        if (slideOffset >= 0.01f) {
            binding.player.root.translationZ = 500f
            binding.bottomNavBar.translationZ = 500f
        } else {
            binding.player.root.translationZ = 0f
            binding.bottomNavBar.translationZ = 0f
        }

        // Animate player shadow. VERIFIED.
        binding.playerShadowExternal.alpha = 1f - slideOffset * 3
        binding.playerShadowInternal.alpha = 1f - slideOffset * 3

        // Animate alpha of the background behind player. VERIFIED.
        binding.background.alpha = slideOffset
    }

    /** Set listeners for the collapsed content of the player. */
    @SuppressLint("ClickableViewAccessibility")
    private fun setCollapsedContentListeners() {



        /** Actions that expand the player. */
        // Expand the player on the frame click.
        binding.player.collapsed.root.setOnClickListener {
            if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        // Expand the player on the play/pause button click.
        binding.player.collapsed.playPause.setOnClickListener {
            Toast.makeText(this, "${(it as ImageView).contentDescription} clicked", Toast.LENGTH_SHORT).show()
        }
        // Expand the player on frame swipe.
        binding.player.collapsed.root
            .setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeTop() {
                if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })
        // Expand the player on play/pause button swipe.
        binding.player.collapsed.playPause
            .setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeTop() {
                if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })
    }

    /** Set listeners for the expanded content of the player. */
    private fun setExpandedContentListeners() {

        /** Slider listeners. */
        binding.player.expanded.slider.addOnChangeListener { slider, value, fromUser ->

        }

        // OnSliderTouchListener is used for animating slider thumb radius.
        val thumbRadiusDefault = resources
                .getDimension(R.dimen.slider_thumb_default).roundToInt()
        val thumbRadiusIncreased = resources
                .getDimension(R.dimen.slider_thumb_increased).roundToInt()

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

        /** Cover listener. */
        // The expanded content of the player is not disabled at application start
        // (because of bug?), so prevent random click on the invisible podcast cover
        // by checking the state of player bottom sheet. If player is collapsed, expand it.
        binding.player.expanded.cover.setOnClickListener {
            if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                Toast.makeText(this, "${(it as ImageView).contentDescription} clicked", Toast.LENGTH_SHORT).show()
            }
        }

        binding.player.expanded.playPause.setOnClickListener {
            Toast.makeText(this, "${(it as ImageView).contentDescription} clicked", Toast.LENGTH_SHORT).show()
        }
        binding.player.expanded.backward.setOnClickListener {
            Toast.makeText(this, "${(it as ImageView).contentDescription} clicked", Toast.LENGTH_SHORT).show()
        }
        binding.player.expanded.forward.setOnClickListener {
            Toast.makeText(this, "${(it as ImageView).contentDescription} clicked", Toast.LENGTH_SHORT).show()
        }
        binding.player.expanded.title.setOnClickListener {
            Toast.makeText(this, "Title clicked", Toast.LENGTH_SHORT).show()
        }
        binding.player.expanded.title.setOnClickListener {
            Toast.makeText(this, "Title clicked", Toast.LENGTH_SHORT).show()
        }
        binding.player.expanded.publisher.setOnClickListener {
            Toast.makeText(this, "Publisher clicked", Toast.LENGTH_SHORT).show()
        }
        binding.player.expanded.speed.setOnClickListener {
            Toast.makeText(this, "Speed clicked", Toast.LENGTH_SHORT).show()
        }
        binding.player.expanded.options.setOnClickListener {
            Toast.makeText(this, "Options clicked", Toast.LENGTH_SHORT).show()
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
            resources.getString(R.string.home) -> navigate(R.id.action_global_homeFragment)
            resources.getString(R.string.explore) -> navigate(R.id.action_global_exploreFragment)
            resources.getString(R.string.activity) -> navigate(R.id.action_global_activityFragment)
        }
    }
}
