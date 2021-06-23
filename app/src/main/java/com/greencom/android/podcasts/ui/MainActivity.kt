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
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.greencom.android.podcasts.player.PLAYER_SKIP_BACKWARD_VALUE
import com.greencom.android.podcasts.player.PLAYER_SKIP_FORWARD_VALUE
import com.greencom.android.podcasts.player.PlayerService
import com.greencom.android.podcasts.ui.activity.ActivityFragment
import com.greencom.android.podcasts.ui.explore.ExploreFragment
import com.greencom.android.podcasts.ui.home.HomeFragment
import com.greencom.android.podcasts.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

// Saving instance state.
private const val STATE_PLAYER_BEHAVIOR = "STATE_PLAYER_BEHAVIOR"

private const val DURATION_SLIDER_THUMB_ANIMATION = 120L
private const val ALPHA_SKIP_HINT_BACKGROUND = 0.5F

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

    /** MainActivityViewModel. */
    private val viewModel: MainActivityViewModel by viewModels()

    /** [PlayerService]'s intent. */
    private val playerServiceIntent: Intent by lazy {
        Intent(this, PlayerService::class.java).apply {
            action = MediaSessionService.SERVICE_INTERFACE
        }
    }

    /** [BottomSheetBehavior] plugin of the player bottom sheet. */
    private lateinit var playerBehavior: BottomSheetBehavior<FrameLayout>

    /** Whether the player bottom sheet is expanded or not. */
    private val isPlayerExpanded: Boolean
        get() = playerBehavior.state == BottomSheetBehavior.STATE_EXPANDED

    /** Whether the player bottom sheet is collapsed or not. */
    private val isPlayerCollapsed: Boolean
        get() = playerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED

    /** Slider thumb animator. */
    private var thumbAnimator: ObjectAnimator? = null

    /**
     * Whether the player bottom sheet is expanded or not, represented as StateFlow.
     * `false` means collapsed. Used to control text marquee animations.
     */
    private val isPlayerExpandedFlow = MutableStateFlow(false)

    // TODO
    private val skipValue = MutableStateFlow(0L)

    // App bar colors.
    private var statusBarColor = 0
    private var navigationBarColorDefault = TypedValue()
    private var navigationBarColorChanged = TypedValue()

    /** ServiceConnection for [PlayerService] binding. */
    private val serviceConnection: ServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d(PLAYER_TAG, "serviceConnection: onServiceConnected()")
                val binder = service as PlayerService.PlayerServiceBinder
                val sessionToken = binder.sessionToken
                viewModel.initPlayerServiceConnection(this@MainActivity, sessionToken)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d(PLAYER_TAG, "serviceConnection: onServiceDisconnected()")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // View binding setup.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        volumeControlStream = AudioManager.STREAM_MUSIC
        startService(playerServiceIntent)

        initViews()
        initNavigation()
        initPlayerListeners()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Control player text marquee animations depending on the bottom sheet state.
                launch {
                    isPlayerExpandedFlow.collectLatest { isPlayerExpanded ->
                        marqueePlayerText(isPlayerExpanded)
                    }
                }

                // TODO
                launch {
                    skipValue.collectLatest { value ->
                        skipBackwardOrForward(value)
                    }
                }

                // TODO
                launch {
                    viewModel.playerState.collect { state ->

                    }
                }

                // TODO
                launch {
                    viewModel.currentEpisode.collectLatest { episode ->

                    }
                }

                // TODO
                launch {
                    viewModel.currentPosition.collect { position ->

                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to PlayerService.
        bindService(playerServiceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        // Unbind from PlayerService and close PlayerServiceConnection to save resources.
        unbindService(serviceConnection)
        viewModel.closePlayerServiceConnection()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PLAYER_BEHAVIOR, playerBehavior.state)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.apply {
            playerBehavior.state = getInt(STATE_PLAYER_BEHAVIOR)
        }

        // Restore player bottom sheet behavior state.
        val slideOffset = if (isPlayerExpanded) 1F else 0F
        val bottomNavBarHeight = resources.getDimension(R.dimen.bottom_nav_bar_height)
        controlPlayerOnBottomSheetStateChanged(playerBehavior.state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            controlPlayerOnBottomSheetSlideV27(slideOffset, bottomNavBarHeight)
        } else {
            controlPlayerOnBottomSheetSlide(slideOffset, bottomNavBarHeight)
        }
    }

    /** Make player closable on back pressed. */
    override fun onBackPressed() {
        if (!isPlayerCollapsed) {
            playerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    /** Make player closable on outside click. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN && !isPlayerCollapsed) {

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
        // Player bottom sheet behavior setup.
        playerBehavior = BottomSheetBehavior.from(binding.player.root).apply {
            setupBottomSheetBehavior()
        }

        // Hide scrim background at start.
        binding.background.isVisible = false
        // Set expanded content alpha to zero.
        expandedPlayer.root.alpha = 0F

        // Initialize player skip hint views.
        expandedPlayer.skipHintBackground.load(R.drawable.skip_hint_background_forward_300px) {
            coverBuilder(this@MainActivity)
        }
        hideSkipHints(true)

        // Set slider's label formatter.
        expandedPlayer.slider.setLabelFormatter { position ->
            Duration.milliseconds(position.toLong()).toComponents { hours, minutes, seconds, _ ->
                return@setLabelFormatter when (hours) {
                    0 -> getString(R.string.player_time_stamp_current_format_m_s, minutes, seconds)
                    else -> getString(R.string.player_time_stamp_current_format_h_m_s, hours, minutes, seconds)
                }
            }
        }

        // Obtain app bar colors.
        statusBarColor = getColor(R.color.background_scrim)
        theme.resolveAttribute(R.attr.colorSurface, navigationBarColorDefault, true)
        theme.resolveAttribute(R.attr.colorBottomSheetBackground, navigationBarColorChanged, true)
    }

    /** Navigation component setup. */
    private fun initNavigation() {
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

    /** Set listeners for the player's content. */
    // TODO: Document after completion
    @SuppressLint("ClickableViewAccessibility")
    private fun initPlayerListeners() {

        // COLLAPSED
        collapsedPlayer.root.setOnClickListener {
            expandPlayer()
        }
        collapsedPlayer.root.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeUp() {
                expandPlayer()
            }
        })
        collapsedPlayer.playPause.setOnTouchListener(object : OnSwipeListener(this) {
            override fun onSwipeUp() {
                expandPlayer()
            }
        })

        collapsedPlayer.playPause.setOnClickListener {

        }


        // EXPANDED.
        expandedPlayer.playPause.setOnClickListener {

        }

        val onTouchListener = object : Slider.OnSliderTouchListener {
            val thumbRadiusDefault = resources.getDimensionPixelSize(R.dimen.player_slider_thumb_default)
            val thumbRadiusIncreased = resources.getDimensionPixelSize(R.dimen.player_slider_thumb_increased)

            override fun onStartTrackingTouch(slider: Slider) {
                animateSliderThumb(thumbRadiusIncreased)
            }

            override fun onStopTrackingTouch(slider: Slider) {
                animateSliderThumb(thumbRadiusDefault)
            }
        }
        expandedPlayer.slider.addOnSliderTouchListener(onTouchListener)

        expandedPlayer.slider.addOnChangeListener { slider, value, _ ->
            expandedPlayer.timeCurrent.text = getCurrentTime(value.toLong())
            expandedPlayer.timeLeft.text = getRemainingTime(
                position = value.toLong(),
                duration = slider.valueTo.toLong(),
            )
        }

        expandedPlayer.skipBackward.setOnClickListener {
            skipValue.value += PLAYER_SKIP_BACKWARD_VALUE
        }

        expandedPlayer.skipForward.setOnClickListener {
            skipValue.value += PLAYER_SKIP_FORWARD_VALUE
        }

        // The expanded content of the player is not disabled at application start
        // (because of bug?), so prevent random click on the invisible podcast cover
        // by checking the state of player bottom sheet. If player is collapsed, expand it.
        expandedPlayer.cover.setOnClickListener {
            if (isPlayerCollapsed) {
                playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
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
        when (newState) {
            BottomSheetBehavior.STATE_EXPANDED -> isPlayerExpandedFlow.value = true
            BottomSheetBehavior.STATE_COLLAPSED -> isPlayerExpandedFlow.value = false
        }

        // Hide the scrim background when the player is collapsed.
        binding.background.isVisible = newState != BottomSheetBehavior.STATE_COLLAPSED
        // Disable the player collapsed content when the player is expanded.
        collapsedPlayer.root.isClickable = newState == BottomSheetBehavior.STATE_COLLAPSED
        // Disable the player expanded content when the player is collapsed.
        expandedPlayer.root.isClickable = newState == BottomSheetBehavior.STATE_EXPANDED
        expandedPlayer.cover.isClickable = newState == BottomSheetBehavior.STATE_EXPANDED
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

    /** Expand the player if it is collapsed. */
    private fun expandPlayer() {
        if (isPlayerCollapsed) {
            playerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
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

    /** Control player text marquee animations depending on the bottom sheet state. */
    private suspend fun marqueePlayerText(isPlayerExpanded: Boolean) {
        if (isPlayerExpanded) {
            collapsedPlayer.title.isSelected = false
            delay(1000) // Delay animation.
            expandedPlayer.title.isSelected = true
            expandedPlayer.publisher.isSelected = true
        } else {
            expandedPlayer.title.isSelected = false
            expandedPlayer.publisher.isSelected = false
            delay(1000) // Delay animation.
            collapsedPlayer.title.isSelected = true
        }
    }

    // TODO
    private suspend fun skipBackwardOrForward(value: Long) {
        if (value == 0L) {
            hideSkipHints()
            return
        }

        if (value > 0) {
            expandedPlayer.skipHintBackward.hideImmediately()
            expandedPlayer.skipHintBackground.rotation = 0F
            expandedPlayer.skipHintForward.text = getSkipHint(value)
            expandedPlayer.skipHintBackground.revealCrossfade(ALPHA_SKIP_HINT_BACKGROUND)
            expandedPlayer.skipHintForward.revealCrossfade()
        } else {
            expandedPlayer.skipHintForward.hideImmediately()
            expandedPlayer.skipHintBackground.rotation = 180F
            expandedPlayer.skipHintBackward.text = getSkipHint(value)
            expandedPlayer.skipHintBackground.revealCrossfade(ALPHA_SKIP_HINT_BACKGROUND)
            expandedPlayer.skipHintBackward.revealCrossfade()
        }

        delay(1000)
        val newValue = (expandedPlayer.slider.value + value).toLong()
        // TODO: Add seekTo().

        hideSkipHints()
        skipValue.value = 0L
    }

    /** Hide all player skip hints. */
    private fun hideSkipHints(immediately: Boolean = false) {
        if (immediately) {
            expandedPlayer.skipHintBackground.hideImmediatelyWithAnimation()
            expandedPlayer.skipHintBackward.hideImmediatelyWithAnimation()
            expandedPlayer.skipHintForward.hideImmediatelyWithAnimation()
        } else {
            expandedPlayer.skipHintBackground.hideCrossfade()
            expandedPlayer.skipHintBackward.hideCrossfade()
            expandedPlayer.skipHintForward.hideCrossfade()
        }
    }

    /** Get a skip hint text for a given value. */
    private fun getSkipHint(value: Long): String {
        val valueInSeconds = abs(value / 1000)
        val minutes = valueInSeconds / 60
        val seconds = valueInSeconds % 60
        return if (minutes == 0L) {
            getString(R.string.player_skip_hint_format_s, seconds)
        } else {
            getString(R.string.player_skip_hint_format_m_s, minutes, seconds)
        }
    }

    /** Converts the current position to a String timestamp that represents the current time. */
    private fun getCurrentTime(position: Long): String {
        Duration.milliseconds(position).toComponents { hours, minutes, seconds, _ ->
            return when (hours) {
                0 -> getString(R.string.player_time_stamp_current_format_m_s, minutes, seconds)
                else -> getString(R.string.player_time_stamp_current_format_h_m_s, hours, minutes, seconds)
            }
        }
    }

    /** Converts the current position to a String timestamp that represents the remaining time. */
    private fun getRemainingTime(position: Long, duration: Long): String {
        Duration.milliseconds(duration - position).toComponents { hours, minutes, seconds, _ ->
            return when (hours) {
                0 -> getString(R.string.player_time_stamp_left_format_m_s, minutes, seconds)
                else -> getString(R.string.player_time_stamp_left_format_h_m_s, hours, minutes, seconds)
            }
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