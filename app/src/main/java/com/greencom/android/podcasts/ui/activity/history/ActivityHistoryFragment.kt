package com.greencom.android.podcasts.ui.activity.history

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
import com.greencom.android.podcasts.databinding.FragmentActivityHistoryBinding
import com.greencom.android.podcasts.ui.activity.ActivityFragmentDirections
import com.greencom.android.podcasts.ui.activity.history.ActivityHistoryViewModel.ActivityHistoryEvent
import com.greencom.android.podcasts.ui.activity.history.ActivityHistoryViewModel.ActivityHistoryState
import com.greencom.android.podcasts.utils.extensions.hideImmediately
import com.greencom.android.podcasts.utils.extensions.revealCrossfade
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val SMOOTH_SCROLL_THRESHOLD = 50

/** Contains a history of completed episodes. */
@AndroidEntryPoint
class ActivityHistoryFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentActivityHistoryBinding? = null
    private val binding get() = _binding!!

    /** ActivityHistoryViewModel. */
    private val viewModel: ActivityHistoryViewModel by viewModels()

    /** RecyclerView history adapter. */
    private val adapter by lazy {
        EpisodeHistoryAdapter(
            navigateToEpisode = viewModel::navigateToEpisode
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load history.
        viewModel.getEpisodeHistory()

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

    /** RecyclerView setup. */
    private fun initRecyclerView() {
        binding.historyList.apply {
            adapter = this@ActivityHistoryFragment.adapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    /** Init fragment result listeners. */
    private fun initFragmentResultListeners() {
        // Scroll the list to the top on tab reselected.
        parentFragmentManager.setFragmentResultListener(
            createOnTabReselectedKey(),
            viewLifecycleOwner
        ) { _, _ ->
            val listLayoutManager = binding.historyList.layoutManager as LinearLayoutManager
            // Smooth scroll or instant scroll depending on the first visible position.
            if (listLayoutManager.findFirstVisibleItemPosition() <= SMOOTH_SCROLL_THRESHOLD) {
                binding.historyList.smoothScrollToPosition(0)
            } else {
                binding.historyList.scrollToPosition(0)
            }
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
                            is ActivityHistoryState.Success -> {
                                showSuccessScreen()
                                adapter.submitList(state.episodes)
                            }

                            // Show Empty screen.
                            ActivityHistoryState.Empty -> showEmptyScreen()
                        }
                    }
                }

                // Observe events.
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            // Navigate to episode page.
                            is ActivityHistoryEvent.NavigateToEpisode -> {
                                findNavController().navigate(
                                    ActivityFragmentDirections.actionActivityFragmentToEpisodeFragment(
                                        event.episodeId
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Show Success screen and hide all others. */
    private fun showSuccessScreen() {
        binding.apply {
            historyList.revealCrossfade()
            emptyScreen.hideImmediately()
        }
    }

    /** Show Empty screen and hide all others. */
    private fun showEmptyScreen() {
        binding.apply {
            historyList.hideImmediately()
            emptyScreen.revealCrossfade()
        }
    }

    /** Hide all screens. */
    private fun hideScreens() {
        binding.apply {
            historyList.hideImmediately()
            emptyScreen.hideImmediately()
        }
    }

    companion object {

        /**
         * Key used to pass data between [ActivityFragment] and [ActivityHistoryFragment]
         * about tab reselection.
         */
        private const val KEY_ON_TAB_RESELECTED = "ACTIVITY_HISTORY_ON_TAB_RESELECTED"

        /**
         * Return key used to pass data between [ActivityFragment] and [ActivityHistoryFragment]
         * about tab reselection.
         */
        fun createOnTabReselectedKey(): String = KEY_ON_TAB_RESELECTED
    }
}