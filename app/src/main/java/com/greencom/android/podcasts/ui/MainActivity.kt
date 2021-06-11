package com.greencom.android.podcasts.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.media2.player.MediaPlayer
import androidx.media2.session.*
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.slider.Slider
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.ActivityMainBinding
import com.greencom.android.podcasts.player.PlayerService
import com.greencom.android.podcasts.player.PlayerViewModel
import com.greencom.android.podcasts.ui.activity.ActivityFragment
import com.greencom.android.podcasts.ui.explore.ExploreFragment
import com.greencom.android.podcasts.ui.home.HomeFragment
import com.greencom.android.podcasts.utils.OnSwipeListener
import com.greencom.android.podcasts.utils.coilDefaultBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

// Saving instance state.
private const val STATE_PLAYER_BEHAVIOR = "STATE_PLAYER_BEHAVIOR"

private const val DURATION_SLIDER_THUMB_ANIMATION = 120L

/**
 * MainActivity is the entry point for the app. This is where the Navigation component,
 * bottom navigation bar, and player bottom sheet are configured.
 */
@ExperimentalTime
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val collapsedPlayer get() = binding.player.collapsed
    private val expandedPlayer get() = binding.player.expanded

    // TODO
    private val playerViewModel: PlayerViewModel by viewModels()

    // TODO
    private var previousPlayedEpisode: PlayerViewModel.EpisodeMetadata? = null

    /** [BottomSheetBehavior] plugin of the player bottom sheet. */
    private lateinit var playerBehavior: BottomSheetBehavior<FrameLayout>

    // App bar colors.
    private var statusBarColor = 0
    private var navigationBarColorDefault = TypedValue()
    private var navigationBarColorChanged = TypedValue()

    // Slider thumb animator.
    private var thumbAnimator: ObjectAnimator? = null

    // TODO
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("serviceConnection: onServiceConnected() called")
            val binder = service as PlayerService.PlayerServiceBinder
            val mediaSessionToken = binder.getSessionToken()
            playerViewModel.createMediaController(this@MainActivity, mediaSessionToken)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("serviceConnection: onServiceDisconnected() called")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // View binding setup.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO
        volumeControlStream = AudioManager.STREAM_MUSIC

        // TODO
        Intent(this, PlayerService::class.java).apply {
            action = MediaSessionService.SERVICE_INTERFACE
        }.also { intent -> bindService(intent, serviceConnection, BIND_AUTO_CREATE) }

        // Player bottom sheet behavior setup.
        playerBehavior = BottomSheetBehavior.from(binding.player.root).apply {
            setupBottomSheetBehavior()
        }

        initViews()
        setupNavigation()
        setPlayerListeners()

        // TODO
        addRepeatingJob(Lifecycle.State.STARTED) {
            playerViewModel.playerState.collect { state ->
                when (state) {
                    MediaPlayer.PLAYER_STATE_PLAYING -> {
                        collapsedPlayer.playPause.setImageResource(R.drawable.ic_pause_24)
                        expandedPlayer.playPause.setImageResource(R.drawable.ic_pause_circle_24)
                    }
                    MediaPlayer.PLAYER_STATE_PAUSED -> {
                        collapsedPlayer.playPause.setImageResource(R.drawable.ic_play_outline_24)
                        expandedPlayer.playPause.setImageResource(R.drawable.ic_play_circle_24)
                    }
                }
            }
        }

        // TODO
        addRepeatingJob(Lifecycle.State.STARTED) {
            playerViewModel.currentEpisode.collectLatest { episode ->
                previousPlayedEpisode?.let { previousEpisode ->
                    if (episode == previousEpisode) return@collectLatest
                }
                previousPlayedEpisode = episode

                collapsedPlayer.title.text = episode.title
                collapsedPlayer.cover.load(episode.image) {
                    coilDefaultBuilder(this@MainActivity)
                }

                expandedPlayer.title.text = episode.title
                expandedPlayer.cover.load(episode.image) {
                    coilDefaultBuilder(this@MainActivity)
                }

                expandedPlayer.slider.valueTo = playerViewModel.duration.toFloat()
                collapsedPlayer.progressBar.max = playerViewModel.duration.toInt()
            }
        }

        // TODO
        addRepeatingJob(Lifecycle.State.STARTED) {
            playerViewModel.currentPosition.collect { position ->
                expandedPlayer.slider.value = position.toFloat()
                collapsedPlayer.progressBar.progress = position.toInt()
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.apply {
            playerBehavior.state = getInt(STATE_PLAYER_BEHAVIOR)
        }

        // Setup player content.
        val slideOffset = if (playerBehavior.state == BottomSheetBehavior.STATE_EXPANDED) 1F else 0F
        val bottomNavBarHeight = resources.getDimension(R.dimen.bottom_nav_bar_height)
        controlPlayerOnBottomSheetStateChanged(playerBehavior.state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            controlPlayerOnBottomSheetSlideV27(slideOffset, bottomNavBarHeight)
        } else {
            controlPlayerOnBottomSheetSlide(slideOffset, bottomNavBarHeight)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(STATE_PLAYER_BEHAVIOR, playerBehavior.state)
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
            playerBehavior.state != BottomSheetBehavior.STATE_COLLAPSED
        ) {

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

    /** Initialize views. */
    private fun initViews() {
        // Set text selected to run ellipsize animation.
        collapsedPlayer.title.isSelected = true
        // Hide scrim background at start.
        binding.background.isVisible = false
        // Set expanded content alpha to zero.
        expandedPlayer.root.alpha = 0F

        // Obtain app bar colors.
        statusBarColor = getColor(R.color.background_scrim)
        theme.resolveAttribute(R.attr.colorSurface, navigationBarColorDefault, true)
        theme.resolveAttribute(R.attr.colorBottomSheetBackground, navigationBarColorChanged, true)

        val thumbRadiusDefault = resources.getDimensionPixelSize(R.dimen.player_slider_thumb_default)
        val thumbRadiusIncreased = resources.getDimensionPixelSize(R.dimen.player_slider_thumb_increased)
        // OnSliderTouchListener is used for animating slider thumb radius.
        val onTouchListener = object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                animateSliderThumb(thumbRadiusIncreased)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                animateSliderThumb(thumbRadiusDefault)
            }
        }
        expandedPlayer.slider.addOnSliderTouchListener(onTouchListener)
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
            val isNavigationNeeded = !currentFragment.isStarting()
            if (isNavigationNeeded) {
                navController.navigateToStartingFragment(it.title)
            }
        }
    }

    /**
     * Set up and add the callback to the player [BottomSheetBehavior] to control
     * the player UI behavior when [BottomSheetBehavior] state and slideOffset change.
     */
    private fun BottomSheetBehavior<FrameLayout>.setupBottomSheetBehavior() {

        val bottomNavBarHeight = resources.getDimension(R.dimen.bottom_nav_bar_height)

        /** BottomSheetCallback for API versions below 27. */
        class Callback : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                controlPlayerOnBottomSheetStateChanged(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                controlPlayerOnBottomSheetSlide(slideOffset, bottomNavBarHeight)
            }
        }

        /** BottomSheetCallback for API versions 27 and higher. */
        @RequiresApi(Build.VERSION_CODES.O_MR1)
        class CallbackV27 : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                controlPlayerOnBottomSheetStateChanged(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                controlPlayerOnBottomSheetSlideV27(slideOffset, bottomNavBarHeight)
            }
        }

        // Add BottomSheetCallback depending on system version.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            addBottomSheetCallback(CallbackV27())
        } else {
            addBottomSheetCallback(Callback())
        }
    }

    /**
     * Control the state of the player content when the player [BottomSheetBehavior]
     * state change.
     */
    private fun controlPlayerOnBottomSheetStateChanged(newState: Int) {
        // Hide the scrim background when the player is collapsed.
        binding.background.isVisible = newState != BottomSheetBehavior.STATE_COLLAPSED
        // Disable the player collapsed content when the player is expanded.
        collapsedPlayer.root.isClickable = newState == BottomSheetBehavior.STATE_COLLAPSED
        // Disable the player expanded content when the player is collapsed.
        expandedPlayer.root.isClickable = newState == BottomSheetBehavior.STATE_EXPANDED
        expandedPlayer.cover.isClickable = newState == BottomSheetBehavior.STATE_EXPANDED
        // Set text selected to run ellipsize animation.
        collapsedPlayer.title.isSelected = newState != BottomSheetBehavior.STATE_EXPANDED
        expandedPlayer.title.isSelected = newState != BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * Animate the player content and background shadows when the player
     * [BottomSheetBehavior] slide offset change. Used on API versions below 27.
     */
    private fun controlPlayerOnBottomSheetSlide(
        slideOffset: Float,
        bottomNavBarHeight: Float
    ) {
        // Animate alpha of the player collapsed content.
        collapsedPlayer.root.alpha = 1F - slideOffset * 10
        collapsedPlayer.progressBar.alpha = 1F - slideOffset * 100

        // Animate alpha of the player expanded content.
        expandedPlayer.root.alpha = (slideOffset * 1.5F) - 0.15F

        // Animate the displacement of the bottomNavBar along the y-axis.
        binding.bottomNavBar.translationY = slideOffset * bottomNavBarHeight * 10

        // Animate player shadow.
        binding.playerShadowExternal.alpha = 1F - slideOffset * 3
        binding.playerShadowInternal.alpha = 1F - slideOffset * 3

        // Animate alpha of the background behind player.
        binding.background.alpha = slideOffset

        // Animate status bar color.
        statusBarColor = ColorUtils
            .setAlphaComponent(statusBarColor, (slideOffset * 255 / 2).roundToInt())
        window.statusBarColor = statusBarColor
    }

    /**
     * Animate the player content and background shadows when the player
     * [BottomSheetBehavior] slide offset change. Used on API versions 27 and higher.
     */
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun controlPlayerOnBottomSheetSlideV27(
        slideOffset: Float,
        bottomNavBarHeight: Float
    ) {
        // Animate alpha of the player collapsed content.
        collapsedPlayer.root.alpha = 1F - slideOffset * 10
        collapsedPlayer.progressBar.alpha = 1F - slideOffset * 100

        // Animate alpha of the player expanded content.
        expandedPlayer.root.alpha = (slideOffset * 1.5F) - 0.15F

        // Animate the displacement of the bottomNavBar along the y-axis.
        binding.bottomNavBar.translationY = slideOffset * bottomNavBarHeight * 10

        // Animate player shadow.
        binding.playerShadowExternal.alpha = 1F - slideOffset * 3
        binding.playerShadowInternal.alpha = 1F - slideOffset * 3

        // Animate alpha of the background behind player.
        binding.background.alpha = slideOffset

        // Animate status bar color.
        statusBarColor = ColorUtils
            .setAlphaComponent(statusBarColor, (slideOffset * 255 / 2).roundToInt())
        window.statusBarColor = statusBarColor

        // Animate navigation bar color.
        if (navigationBarColorDefault.data != navigationBarColorChanged.data) {
            if (slideOffset >= 0.1 && window.navigationBarColor != navigationBarColorChanged.data) {
                window.navigationBarColor = navigationBarColorChanged.data
            } else if (slideOffset < 0.1 && window.navigationBarColor != navigationBarColorDefault.data) {
                window.navigationBarColor = navigationBarColorDefault.data
            }
        }
    }

    /** Set listeners for the player's content. */
    @SuppressLint("ClickableViewAccessibility")
    private fun setPlayerListeners() {

        // COLLAPSED
        // Expand the player on the frame click.
        collapsedPlayer.root.setOnClickListener {
            expandPlayer()
        }
        // Expand the player on frame swipe.
        collapsedPlayer.root.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeUp() {
                expandPlayer()
            }
        })
        // Expand the player on play/pause button swipe.
        collapsedPlayer.playPause.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeUp() {
                expandPlayer()
            }
        })

        collapsedPlayer.playPause.setOnClickListener {
            when {
                playerViewModel.isPlaying -> playerViewModel.pause()
                playerViewModel.isPaused -> playerViewModel.play()
            }
        }


        // EXPANDED.
        expandedPlayer.playPause.setOnClickListener {
            when {
                playerViewModel.isPlaying -> playerViewModel.pause()
                playerViewModel.isPaused -> playerViewModel.play()
            }
        }

        expandedPlayer.slider.addOnChangeListener { _, _, _ ->

        }

        expandedPlayer.backward.setOnClickListener {

        }

        expandedPlayer.forward.setOnClickListener {

        }

        // The expanded content of the player is not disabled at application start
        // (because of bug?), so prevent random click on the invisible podcast cover
        // by checking the state of player bottom sheet. If player is collapsed, expand it.
        expandedPlayer.cover.setOnClickListener {
            if (playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {

            }
        }

        expandedPlayer.title.setOnClickListener {

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

    /** Animate slider thumb radius to a given value. */
    private fun animateSliderThumb(to: Int) {
        if (thumbAnimator != null) {
            thumbAnimator?.setIntValues(to)
        } else {
            thumbAnimator = ObjectAnimator.ofInt(
                expandedPlayer.slider,
                "thumbRadius",
                to
            ).apply {
                duration = DURATION_SLIDER_THUMB_ANIMATION
                setAutoCancel(true)
            }
        }
        thumbAnimator?.start()
    }
}