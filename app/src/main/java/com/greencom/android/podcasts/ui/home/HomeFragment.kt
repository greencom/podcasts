package com.greencom.android.podcasts.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentHomeBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog
import com.greencom.android.podcasts.utils.AppBarLayoutStateChangeListener
import com.greencom.android.podcasts.utils.extensions.*
import com.greencom.android.podcasts.utils.setAppBarLayoutCanDrag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val SUBSCRIPTION_MODE_GRID_COVER_ONLY = 1
const val SUBSCRIPTION_MODE_GRID_WITH_TITLE = 2

// Saving instance state.
private const val SAVED_STATE_IS_APP_BAR_EXPANDED = "IS_APP_BAR_EXPANDED"

/** Represents a list of the user subscriptions. */
@AndroidEntryPoint
class HomeFragment : Fragment(), UnsubscribeDialog.UnsubscribeDialogListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    /** RecyclerView adapter used in the [SUBSCRIPTION_MODE_GRID_COVER_ONLY] mode. */
    private var adapterGridCoverOnly: SubscriptionsPodcastCoverOnlyAdapter? = null

    /** RecyclerView adapter used in the [SUBSCRIPTION_MODE_GRID_WITH_TITLE] mode. */
    private var adapterGridWithTitle: SubscriptionsPodcastWithTitleAdapter? = null

    /** Whether the app bar is expanded or not. */
    private var isAppBarExpanded = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaterialSharedAxisTransitions()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SwitchIntDef")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        binding.root.doOnPreDraw { startPostponedEnterTransition() }

        // Restore instance state.
        savedInstanceState?.apply {
            binding.appBarLayout.setExpanded(getBoolean(SAVED_STATE_IS_APP_BAR_EXPANDED), false)
        }

        // Hide all screens at start.
        hideScreens()

        // Load subscriptions.
        viewModel.getSubscriptions()

        initAppBar()
        initRecyclerView()
        initObservers()
        initFragmentResultListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putBoolean(SAVED_STATE_IS_APP_BAR_EXPANDED, isAppBarExpanded)
        }
    }

    // Unsubscribe from the podcast if the user confirms in the UnsubscribeDialog.
    override fun onUnsubscribe(podcastId: String) {
        viewModel.unsubscribe(podcastId)
    }

    /** App bar setup. */
    private fun initAppBar() {
        setAppBarLayoutCanDrag(binding.appBarLayout, false)

        // Track app bar state.
        binding.appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, newState: Int) {
                when (newState) {
                    EXPANDED -> isAppBarExpanded = true
                    COLLAPSED -> isAppBarExpanded = false
                }
            }
        })

        // Navigate to SettingsFragment.
        binding.appBarOptions.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
            )
        }
    }

    /** RecyclerView setup. */
    private fun initRecyclerView() {
        val onScrollListener = object : RecyclerView.OnScrollListener() {
            val layoutManager = binding.recyclerView.layoutManager as GridLayoutManager
            var firstCompletelyVisibleItemPosition = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()

                // Show and hide app bar divider.
                binding.appBarDivider.apply {
                    if (firstCompletelyVisibleItemPosition > 0) revealImmediately() else hideCrossfade()
                }
            }
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)
            addOnScrollListener(onScrollListener)
        }
    }

    /** Set observers for ViewModel observables. */
    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI states.
                launch {
                    viewModel.uiState.collectLatest { state ->
                        when (state) {
                            // Show Success screen.
                            is HomeViewModel.HomeState.Success -> {
                                showSuccessScreen()
                                adapterGridCoverOnly?.submitList(state.podcasts)
                                adapterGridWithTitle?.submitList(state.podcasts)
                            }

                            // Show Empty screen.
                            HomeViewModel.HomeState.Empty -> showEmptyScreen()
                        }
                    }
                }

                // Observe subscription presentation mode.
                launch {
                    viewModel.getSubscriptionMode().collect { mode ->
                        handleSubscriptionMode(mode ?: SUBSCRIPTION_MODE_GRID_WITH_TITLE)
                    }
                }

                // Observe events.
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            // Navigate to a podcast page.
                            is HomeViewModel.HomeEvent.NavigateToPodcast -> {
                                findNavController().navigate(
                                    HomeFragmentDirections.actionHomeFragmentToPodcastFragment(
                                        event.podcastId
                                    )
                                )
                            }

                            // Show an UnsubscribeDialog.
                            is HomeViewModel.HomeEvent.UnsubscribeDialog -> {
                                UnsubscribeDialog.show(childFragmentManager, event.podcastId)
                            }
                        }
                    }
                }
            }
        }
    }

    /** Init fragment result listeners. */
    private fun initFragmentResultListeners() {
        // Scroll the list to the top when the corresponding bottom nav item reselected.
        parentFragmentManager.setFragmentResultListener(
            createOnNavItemReselectedKey(),
            viewLifecycleOwner
        ) { _, _ ->
            binding.recyclerView.smoothScrollToPosition(0)
            binding.appBarLayout.setExpanded(true, true)
        }
    }

    /**
     * Handle subscription presentation mode. This method changes RecyclerView adapters
     * to present subscription list in the appropriate way.
     */
    private fun handleSubscriptionMode(mode: Int) {
        binding.recyclerView.apply {
            when (mode) {
                SUBSCRIPTION_MODE_GRID_COVER_ONLY -> {
                    // The required adapter is already initialized, just set it to the RV.
                    if (adapterGridCoverOnly != null) {
                        setPadding(0, 0, 0, 0) // Update RV paddings.
                        adapter = adapterGridCoverOnly
                        return
                    }

                    // Clear the unnecessary adapter.
                    adapterGridWithTitle = null
                    // Initialize the correct adapter.
                    adapterGridCoverOnly = SubscriptionsPodcastCoverOnlyAdapter(
                        navigateToPodcast = viewModel::navigateToPodcast,
                        showUnsubscribeDialog = viewModel::showUnsubscribeDialog,
                    ).apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        // Restore the last UI state.
                        val lastUiState = viewModel.uiState.value
                        if (lastUiState is HomeViewModel.HomeState.Success) {
                            submitList(lastUiState.podcasts)
                        }
                    }

                    setPadding(0, 0, 0, 0)
                    adapter = adapterGridCoverOnly
                }

                SUBSCRIPTION_MODE_GRID_WITH_TITLE -> {
                    // The required adapter is already initialized, just set it to the RV.
                    if (adapterGridWithTitle != null) {
                        val horizontalPadding = resources.getDimensionPixelOffset(R.dimen.home_recycler_view_padding) // Update RV paddings.
                        setPadding(horizontalPadding, 0, horizontalPadding, 0)
                        adapter = adapterGridWithTitle
                        return
                    }

                    // Clear the unnecessary adapter.
                    adapterGridCoverOnly = null
                    // Initialize the correct adapter.
                    adapterGridWithTitle = SubscriptionsPodcastWithTitleAdapter(
                        navigateToPodcast = viewModel::navigateToPodcast,
                        showUnsubscribeDialog = viewModel::showUnsubscribeDialog,
                    ).apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        // Restore the last UI state.
                        val lastUiState = viewModel.uiState.value
                        if (lastUiState is HomeViewModel.HomeState.Success) {
                            submitList(lastUiState.podcasts)
                        }
                    }

                    val horizontalPadding = resources.getDimensionPixelOffset(R.dimen.home_recycler_view_padding)
                    setPadding(horizontalPadding, 0, horizontalPadding, 0)
                    adapter = adapterGridWithTitle
                }
            }
        }
    }

    /** Show Success screen and hide all others. */
    private fun showSuccessScreen() {
        binding.apply {
            emptyScreen.hideImmediately()
            recyclerView.revealCrossfade()
        }
    }

    /** Show Empty screen and hide all others. */
    private fun showEmptyScreen() {
        binding.apply {
            recyclerView.hideImmediately()
            emptyScreen.revealCrossfade()
        }
    }

    /** Hide all screens. */
    private fun hideScreens() {
        binding.apply {
            recyclerView.hideImmediately()
            emptyScreen.hideImmediately()
        }
    }

    companion object {
        /**
         * Key used to pass data between [MainActivity][com.greencom.android.podcasts.ui.MainActivity]
         * and [HomeFragment] about the reselection of the corresponding nav item.
         */
        private const val KEY_ON_NAV_ITEM_RESELECTED = "HOME_ON_NAV_ITEM_RESELECTED"

        /**
         * Returns key used to pass data between
         * [MainActivity][com.greencom.android.podcasts.ui.MainActivity] and [HomeFragment]
         * about the reselection of the corresponding nav item.
         */
        fun createOnNavItemReselectedKey(): String = KEY_ON_NAV_ITEM_RESELECTED
    }
}