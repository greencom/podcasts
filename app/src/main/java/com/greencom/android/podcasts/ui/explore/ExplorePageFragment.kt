package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
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
import com.greencom.android.podcasts.utils.reveal
import com.greencom.android.podcasts.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

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

    /** RecyclerView adapter. */
    private val adapter: ExplorePodcastAdapter by lazy {
        ExplorePodcastAdapter(viewModel::navigateToPodcast, viewModel::updateSubscription)
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
        // Get the genre ID from the fragment arguments.
        val genreId = arguments?.getInt(GENRE_ID) ?: 0
        // Load the best podcasts.
        viewModel.getBestPodcasts(genreId)

        setupRecyclerView()
        setupSwipeToRefresh()
        setupContentAlpha()

        setObservers()
        setFragmentResultListeners(genreId)
        setViewListeners(genreId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** RecyclerView setup. */
    private fun setupRecyclerView() {
        val divider = CustomDividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        divider.setDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.shape_divider, context?.theme)!!
        )
        binding.podcastList.apply {
            adapter = this@ExplorePageFragment.adapter
            addItemDecoration(divider)
        }
    }

    /** Swipe-to-refresh setup. */
    private fun setupSwipeToRefresh() {
        binding.swipeToRefresh.apply {
            val color = TypedValue()
            val backgroundColor = TypedValue()
            val theme = context?.theme
            theme?.resolveAttribute(
                R.attr.colorSwipeToRefreshBackground, backgroundColor, true
            )
            theme?.resolveAttribute(R.attr.colorPrimary, color, true)
            setProgressBackgroundColorSchemeColor(backgroundColor.data)
            setColorSchemeColors(color.data)
        }
    }

    /** Set up alpha of fragment views. Used to create crossfade animations on [reveal]. */
    private fun setupContentAlpha() {
        binding.error.root.alpha = 0f
        binding.podcastList.alpha = 0f
    }

    /** Set observers for ViewModel observables. */
    private fun setObservers() {
        // Observe UI states.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.uiState.collect { state ->
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
    private fun setFragmentResultListeners(genreId: Int) {
        // Scroll the list of the best podcasts on tab reselection.
        parentFragmentManager.setFragmentResultListener(
            "$ON_TAB_RESELECTED$genreId",
            viewLifecycleOwner
        ) { _, _ ->
            binding.podcastList.smoothScrollToPosition(0)
        }
    }

    /** Set fragment's view on touch listeners. */
    private fun setViewListeners(genreId: Int) {
        // Fetch the podcasts from the error screen.
        binding.error.tryAgain.setOnClickListener {
            viewModel.fetchBestPodcasts(genreId)
        }

        // Refresh the podcasts with swipe-to-refresh gesture.
        binding.swipeToRefresh.setOnRefreshListener {
            viewModel.refreshBestPodcasts(genreId, adapter.currentList)
        }
    }

    /** Handle UI states. */
    private fun handleUiState(state: ExplorePageState) {
        binding.loading.isVisible = state is ExplorePageState.Loading
        binding.swipeToRefresh.isVisible = state is ExplorePageState.Success
        binding.error.root.isVisible = state is ExplorePageState.Error

        when (state) {
            is ExplorePageState.Success -> {
                adapter.submitList(state.podcasts)
                binding.podcastList.reveal()
                // Reset error screen alpha.
                binding.error.root.alpha = 0f
            }
            is ExplorePageState.Error -> {
                binding.error.root.reveal()
                // Reset podcast list alpha.
                binding.podcastList.alpha = 0f
            }

            // Make `when` expression exhaustive.
            is ExplorePageState.Loading -> {  }
        }
    }

    /** Handle events. */
    private suspend fun handleEvent(event: ExplorePageEvent) {
        binding.swipeToRefresh.isRefreshing = event is ExplorePageEvent.Refreshing
        binding.error.tryAgain.isEnabled = event !is ExplorePageEvent.Fetching

        when (event) {

            // Show a snackbar.
            is ExplorePageEvent.Snackbar -> showSnackbar(binding.root, event.stringRes)

            // Show UnsubscribeDialog.
            is ExplorePageEvent.UnsubscribeDialog ->
                UnsubscribeDialog.show(childFragmentManager, event.podcastId)

            // Navigate to PodcastFragment.
            is ExplorePageEvent.NavigateToPodcast -> {
                findNavController().navigate(
                    ExploreFragmentDirections.actionExploreFragmentToPodcastFragment(
                        event.podcastId
                    )
                )
            }

            // Scroll the list up when refreshed.
            is ExplorePageEvent.Refreshed -> {
                showSnackbar(binding.root, event.stringRes)
                if (event.isSuccessful) {
                    // Wait for a list to update to scroll up.
                    delay(500)
                    binding.podcastList.smoothScrollToPosition(0)
                }
            }

            // Make `when` expression exhaustive.
            is ExplorePageEvent.Fetching -> {  }
            is ExplorePageEvent.Refreshing -> {  }
        }
    }

    // Unsubscribe from the podcast if the user confirms in the UnsubscribeDialog.
    override fun onUnsubscribeClick(podcastId: String) {
        viewModel.unsubscribe(podcastId)
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