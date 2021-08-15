package com.greencom.android.podcasts.ui.activity.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.greencom.android.podcasts.databinding.FragmentActivityBookmarksAndProgressBinding
import com.greencom.android.podcasts.ui.activity.ActivityEpisodeAdapter
import com.greencom.android.podcasts.ui.activity.ActivityFragmentDirections
import com.greencom.android.podcasts.ui.activity.bookmarks.ActivityBookmarksViewModel.ActivityBookmarksEvent
import com.greencom.android.podcasts.ui.activity.bookmarks.ActivityBookmarksViewModel.ActivityBookmarksState
import com.greencom.android.podcasts.ui.dialogs.EpisodeOptionsDialog
import com.greencom.android.podcasts.utils.extensions.hideImmediately
import com.greencom.android.podcasts.utils.extensions.revealCrossfade
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val SMOOTH_SCROLL_THRESHOLD = 50

/** Contains a list of episodes that have been added to the bookmarks. */
@AndroidEntryPoint
class ActivityBookmarksFragment : Fragment(), EpisodeOptionsDialog.EpisodeOptionsDialogListener {

    private var _binding: FragmentActivityBookmarksAndProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivityBookmarksViewModel by viewModels()

    /** RecyclerView adapter. */
    private val adapter by lazy {
        ActivityEpisodeAdapter(
            dateMode = ActivityEpisodeAdapter.MODE_PUB_DATE,
            navigateToEpisode = viewModel::navigateToEpisode,
            onInBookmarksChange = viewModel::onInBookmarksChange,
            setEpisode = viewModel::setEpisode,
            play = viewModel::play,
            pause = viewModel::pause,
            showEpisodeOptions = viewModel::showEpisodeOptions
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityBookmarksAndProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load bookmarks.
        viewModel.getBookmarks()

        // Hide all screens at start.
        hideScreens()

        initRecyclerView()
        initObservers()
        initFragmentResultListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Mark episode as completed or uncompleted when the user performs action
    // in the EpisodeOptionsDialog.
    override fun onEpisodeOptionsIsCompletedChange(
        episodeId: String,
        isCompleted: Boolean
    ) {
        viewModel.onIsCompletedChange(episodeId, isCompleted)
    }

    /** RecyclerView setup. */
    private fun initRecyclerView() {
        binding.recyclerView.apply {
            setHasFixedSize(true)
            adapter = this@ActivityBookmarksFragment.adapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
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
                            is ActivityBookmarksState.Success -> {
                                showSuccessScreen()
                                adapter.submitList(state.episodes)
                            }

                            // Show Empty screen.
                            ActivityBookmarksState.Empty -> showEmptyScreen()
                        }
                    }
                }

                // Observe events.
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            // Navigate to episode page.
                            is ActivityBookmarksEvent.NavigateToEpisode -> {
                                findNavController().navigate(
                                    ActivityFragmentDirections.actionActivityFragmentToEpisodeFragment(
                                        event.episodeId
                                    )
                                )
                            }

                            // Show an EpisodeOptionsDialog.
                            is ActivityBookmarksEvent.EpisodeOptionDialog -> {
                                EpisodeOptionsDialog.show(
                                    fragmentManager = childFragmentManager,
                                    episodeId = event.episodeId,
                                    isEpisodeCompleted = event.isEpisodeCompleted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Init fragment result listeners. */
    private fun initFragmentResultListeners() {
        // Scroll the list to the top on tab reselected.
        parentFragmentManager.setFragmentResultListener(
            createOnTabReselectedKey(),
            viewLifecycleOwner
        ) { _, _ ->
            val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
            // Smooth scroll or instant scroll depending on the first visible position.
            if (layoutManager.findFirstVisibleItemPosition() <= SMOOTH_SCROLL_THRESHOLD) {
                binding.recyclerView.smoothScrollToPosition(0)
            } else {
                binding.recyclerView.scrollToPosition(0)
            }
        }
    }

    /** Show Success screen and hide all others. */
    private fun showSuccessScreen() {
        binding.apply {
            emptyImage.hideImmediately()
            emptyMessage.hideImmediately()
            recyclerView.revealCrossfade()
        }
    }

    /** Show Empty screen and hide all others. */
    private fun showEmptyScreen() {
        binding.apply {
            recyclerView.hideImmediately()
            emptyImage.revealCrossfade()
            emptyMessage.revealCrossfade()
        }
    }

    /** Hide all screens. */
    private fun hideScreens() {
        binding.apply {
            recyclerView.hideImmediately()
            emptyImage.hideImmediately()
            emptyMessage.hideImmediately()
        }
    }

    companion object {

        /**
         * Key used to pass data between
         * [ActivityFragment][com.greencom.android.podcasts.ui.activity.ActivityFragment]
         * and [ActivityBookmarksFragment] about tab reselection.
         */
        private const val KEY_ON_TAB_RESELECTED = "ACTIVITY_BOOKMARKS_ON_TAB_RESELECTED"

        /**
         * Return key used to pass data between
         * [ActivityFragment][com.greencom.android.podcasts.ui.activity.ActivityFragment]
         * and [ActivityBookmarksFragment] about tab reselection.
         */
        fun createOnTabReselectedKey(): String = KEY_ON_TAB_RESELECTED
    }
}