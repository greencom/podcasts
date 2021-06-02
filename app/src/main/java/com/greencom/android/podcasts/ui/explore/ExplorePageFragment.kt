package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExplorePageBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog
import com.greencom.android.podcasts.ui.explore.ExploreViewModel.*
import com.greencom.android.podcasts.utils.CustomDividerItemDecoration
import com.greencom.android.podcasts.utils.revealCrossfade
import com.greencom.android.podcasts.utils.setupSwipeToRefresh
import com.greencom.android.podcasts.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

// Initialization parameters.
private const val GENRE_ID = "genre_id"

/**
 * Represents all pages of the ViewPager2 in the ExploreFragment. Each page contains
 * a list of the best podcasts for the specified genre.
 *
 * Use [ExplorePageFragment.newInstance] to create a new instance
 * of the fragment with provided parameters.
 */
@AndroidEntryPoint
class ExplorePageFragment : Fragment(), UnsubscribeDialog.UnsubscribeDialogListener {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentExplorePageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by viewModels()

    /** Genre ID associated with this fragment. */
    var genreId = 0

    /** RecyclerView adapter. */
    private val adapter: BestPodcastAdapter by lazy {
        BestPodcastAdapter(viewModel::navigateToPodcast, viewModel::updateSubscription)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentExplorePageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Start ExploreFragment postponed transition.
        binding.root.doOnPreDraw { parentFragment?.startPostponedEnterTransition() }

        // Get the genre ID from the fragment arguments.
        genreId = arguments?.getInt(GENRE_ID) ?: 0

        // Load the best podcasts.
        viewModel.getBestPodcasts(genreId)

        setupRecyclerView()
        setupViews()

        setObservers()
        setFragmentResultListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    // Unsubscribe from the podcast if the user confirms in the UnsubscribeDialog.
    override fun onUnsubscribeClick(podcastId: String) {
        viewModel.unsubscribe(podcastId)
    }

    /** RecyclerView setup. */
    private fun setupRecyclerView() {
        val divider = CustomDividerItemDecoration(requireContext())
        divider.setDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.shape_divider, context?.theme)!!
        )
        binding.podcastList.apply {
            adapter = this@ExplorePageFragment.adapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            addItemDecoration(divider)
        }

        setupSwipeToRefresh(binding.swipeToRefresh, requireContext())
    }

    /** Fragment views setup. */
    private fun setupViews() {
        // Set alpha to create crossfade animations on reveal.
        hideSuccessScreen()
        hideErrorScreen()

        // Fetch the best podcasts from the error screen.
        binding.error.tryAgain.setOnClickListener { viewModel.fetchBestPodcasts(genreId) }

        // Refresh the podcasts with swipe-to-refresh gesture.
        binding.swipeToRefresh.setOnRefreshListener {
            viewModel.refreshBestPodcasts(genreId, adapter.currentList)
        }
    }

    /** Set observers for ViewModel observables. */
    private fun setObservers() {
        // Observe UI states.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.uiState.collectLatest { state ->
                handleUiState(state)
            }
        }

        // Observe events.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.event.collect { event ->
                handleEvent(event)
            }
        }
    }

    /** Set fragment result listeners. */
    private fun setFragmentResultListeners() {
        // Scroll the list of the best podcasts on tab reselection.
        parentFragmentManager.setFragmentResultListener(
            "$ON_TAB_RESELECTED$genreId",
            viewLifecycleOwner
        ) { _, _ ->
            binding.podcastList.smoothScrollToPosition(0)
        }
    }

    /** Handle UI states. */
    private fun handleUiState(state: ExplorePageState) {
        binding.swipeToRefresh.isVisible = state is ExplorePageState.Success
        binding.error.root.isVisible = state is ExplorePageState.Error
        binding.loading.isVisible = state is ExplorePageState.Loading

        when (state) {

            // Show podcast list.
            is ExplorePageState.Success -> {
                adapter.submitList(state.podcasts)
                binding.podcastList.revealCrossfade()
                hideErrorScreen()
            }

            // Show error screen.
            is ExplorePageState.Error -> {
                // Position the progress bar depending on ExplorePodcast app bar state.
                if ((parentFragment as ExploreFragment).isAppBarExpanded) {
                    // TODO: Use the appropriate value after app bar rework.
                    binding.error.progressBar.translationY = -(resources.getDimension(R.dimen.bottom_nav_bar_height))
                } else {
                    binding.error.progressBar.translationY = 0F
                }

                binding.error.root.revealCrossfade()
                hideSuccessScreen()
            }

            // Make `when` expression exhaustive.
            is ExplorePageState.Loading -> {  }
        }
    }

    /** Handle events. */
    private fun handleEvent(event: ExplorePageEvent) {
        binding.swipeToRefresh.isRefreshing = event is ExplorePageEvent.Refreshing
        binding.error.tryAgain.isEnabled = event !is ExplorePageEvent.Fetching
        binding.error.progressBar.isVisible = event is ExplorePageEvent.Fetching

        // Change 'Try again' button text.
        if (event is ExplorePageEvent.Fetching) {
            binding.error.tryAgain.text = getString(R.string.loading)
        } else {
            binding.error.tryAgain.text = getString(R.string.try_again)
        }

        when (event) {

            // Show a snackbar.
            is ExplorePageEvent.Snackbar -> showSnackbar(binding.root, event.stringRes)

            // Show UnsubscribeDialog.
            is ExplorePageEvent.UnsubscribeDialog ->
                UnsubscribeDialog.show(childFragmentManager, event.podcastId)

            // Navigate to PodcastFragment.
            is ExplorePageEvent.NavigateToPodcast -> findNavController().navigate(
                ExploreFragmentDirections.actionExploreFragmentToPodcastFragment(
                    event.podcastId
                )
            )

            // Show Loading process.
            is ExplorePageEvent.Fetching -> binding.error.progressBar.revealCrossfade()

            // Make `when` expression exhaustive.
            is ExplorePageEvent.Refreshing -> {  }
        }
    }

    /** Set alpha of the success screen to 0. */
    private fun hideSuccessScreen() {
        binding.podcastList.alpha = 0F
    }

    /** Set alpha of the error screen to 0. */
    private fun hideErrorScreen() {
        binding.error.root.alpha = 0F
    }

    companion object {

        /**
         * Key prefix used to pass and retrieve data about reselected tab
         * in [ExploreFragment] between fragments.
         */
        const val ON_TAB_RESELECTED = "explore_page_on_tab_reselected"

        /**
         * Use this factory method to create a new instance of
         * the fragment using the provided parameters.
         *
         * @param genreId ID of the genre.
         * @return A new instance of [ExplorePageFragment].
         */
        fun newInstance(genreId: Int) = ExplorePageFragment().apply {
            arguments = Bundle().apply {
                putInt(GENRE_ID, genreId)
            }
        }
    }
}