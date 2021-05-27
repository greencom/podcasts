package com.greencom.android.podcasts.ui.podcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentPodcastBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastEvent
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastState
import com.greencom.android.podcasts.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.TimeUnit

private const val SCROLL_TO_TOP_THRESHOLD = 5

@AndroidEntryPoint
class PodcastFragment : Fragment(), UnsubscribeDialog.UnsubscribeDialogListener {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentPodcastBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** PodcastViewModel. */
    private val viewModel: PodcastViewModel by viewModels()

    /** Navigation Safe Args. */
    private val args: PodcastFragmentArgs by navArgs()

    /** Podcast ID. */
    private var id = ""

    /** RecyclerView adapter. */
    private val adapter: PodcastWithEpisodesAdapter by lazy {
        PodcastWithEpisodesAdapter(
            viewModel.sortOrder,
            viewModel::updateSubscription,
            viewModel::changeSortOrder
        )
    }

    /** Whether the app bar is collapsed or not. */
    private var isAppBarCollapsed = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentPodcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition(100L, TimeUnit.MILLISECONDS)

        // Restore instance state.
        savedInstanceState?.apply {
            binding.appBarLayout.setExpanded(!getBoolean(STATE_APP_BAR), false)
            binding.scrollToTop.apply { if (getBoolean(STATE_SCROLL_TO_TOP)) show() else hide() }
        }

        // Get the podcast ID from the navigation arguments.
        id = args.podcastId

        // Load a podcast with episodes.
        viewModel.getPodcastWithEpisodes(id)

        setupAppBar()
        setupRecyclerView()
        setupViews()

        setObservers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putBoolean(STATE_APP_BAR, isAppBarCollapsed)
            putBoolean(STATE_SCROLL_TO_TOP, binding.scrollToTop.isOrWillBeShown)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel adapter coroutine scope in onDetachedFromRecyclerView().
        adapter.onDetachedFromRecyclerView(binding.list)
        // Clear View binding.
        _binding = null
    }

    // Unsubscribe from the podcast if the user confirms in the UnsubscribeDialog.
    override fun onUnsubscribeClick(podcastId: String) {
        viewModel.unsubscribe(podcastId)
    }

    /** App bar setup. */
    private fun setupAppBar() {
        // Disable AppBarLayout dragging behavior.
        if (binding.appBarLayout.layoutParams != null) {
            val appBarParams = binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            val appBarBehavior = AppBarLayout.Behavior()
            appBarBehavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                    return false
                }
            })
            appBarParams.behavior = appBarBehavior
        }

        // Monitoring app bar state.
        binding.appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                isAppBarCollapsed = verticalOffset != 0
            })
    }

    /** RecyclerView setup. */
    private fun setupRecyclerView() {
        val divider = CustomDividerItemDecoration(requireContext(), true)
        divider.setDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.shape_divider, context?.theme)!!
        )

        val onScrollListener = object : RecyclerView.OnScrollListener() {
            val layoutManager = binding.list.layoutManager as LinearLayoutManager
            var visibleItemCount = 0
            var totalItemCount = 0
            var firstVisibleItemPosition = 0
            var initialCheckSkipped = false

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                visibleItemCount = layoutManager.childCount
                totalItemCount = layoutManager.itemCount
                firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Show and hide the fab. Skip the initial check to restore instance state.
                if (initialCheckSkipped) {
                    binding.scrollToTop.apply {
                        if (firstVisibleItemPosition >= SCROLL_TO_TOP_THRESHOLD && dy < 0) {
                            show()
                        } else {
                            hide()
                        }
                    }
                } else {
                    initialCheckSkipped = true
                }
            }
        }

        binding.list.apply {
            adapter = this@PodcastFragment.adapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            addItemDecoration(divider)
            addOnScrollListener(onScrollListener)
        }

        setupSwipeToRefresh(binding.swipeToRefresh, requireContext())
    }

    /** Fragment views setup. */
    private fun setupViews() {
        hideErrorScreen()

        // Handle toolbar back button clicks.
        binding.toolbarBack.setOnClickListener { findNavController().navigateUp() }

        // TODO
        binding.swipeToRefresh.setOnRefreshListener {

        }

        // Scroll to top and hide the fab.
        binding.scrollToTop.setOnClickListener {
            binding.list.smoothScrollToPosition(0)
            binding.appBarLayout.setExpanded(true, true)
        }

        // Fetch the podcast from the error screen.
        binding.error.tryAgain.setOnClickListener { viewModel.fetchPodcast(id) }
    }

    /** Set observers for ViewModel observables. */
    private fun setObservers() {
        // Observe UI states.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
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

    /** Handle UI states. */
    private fun handleUiState(state: PodcastState) {
        binding.swipeToRefresh.isVisible = state is PodcastState.Success
        binding.error.root.isVisible = state is PodcastState.Error
        binding.loading.isVisible = state is PodcastState.Loading

        when (state) {

            // Show podcast data.
            is PodcastState.Success -> {
                adapter.submitHeaderAndList(
                    state.podcastWithEpisodes.podcast,
                    state.podcastWithEpisodes.episodes
                )
                binding.list.revealCrossfade()
                hideErrorScreen()
            }

            // Show error screen.
            is PodcastState.Error -> {
                binding.error.root.revealCrossfade()
                hideSuccessScreen()
            }

            // Make `when` expression exhaustive.
            is PodcastState.Loading -> {  }
        }
    }

    /** Handle events. */
    private fun handleEvent(event: PodcastEvent) {
        binding.error.tryAgain.isEnabled = event !is PodcastEvent.Fetching
        binding.error.progressBar.isVisible = event is PodcastEvent.Fetching

        // Change 'Try again' button text.
        if (event is PodcastEvent.Fetching) {
            binding.error.tryAgain.text = getString(R.string.explore_loading)
        } else {
            binding.error.tryAgain.text = getString(R.string.explore_try_again)
        }

        when (event) {

            // Show a snackbar.
            is PodcastEvent.Snackbar -> showSnackbar(binding.root, event.stringRes)

            // Show UnsubscribeDialog.
            is PodcastEvent.UnsubscribeDialog ->
                UnsubscribeDialog.show(childFragmentManager, id)

            // Show Loading process.
            is PodcastEvent.Fetching -> binding.error.progressBar.revealCrossfade()

            // Show episodes fetching progress bar.
            is PodcastEvent.EpisodesFetchingStarted ->
                binding.episodesProgressBar.revealImmediately()

            // Hide episodes fetching progress bar.
            is PodcastEvent.EpisodesFetchingFinished ->
                binding.episodesProgressBar.hideCrossfade()
        }
    }

    /** Set alpha of the success screen to 0. */
    private fun hideSuccessScreen() {
        binding.list.alpha = 0f
    }

    /** Set alpha of the error screen to 0. */
    private fun hideErrorScreen() {
        binding.error.root.alpha = 0f
    }

    companion object {
        // Saving instance state.
        private const val STATE_APP_BAR = "app_bar_state"
        private const val STATE_SCROLL_TO_TOP = "scroll_to_top_state"
    }
}