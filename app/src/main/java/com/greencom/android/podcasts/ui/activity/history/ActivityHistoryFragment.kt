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
import androidx.recyclerview.widget.RecyclerView
import com.greencom.android.podcasts.databinding.FragmentActivityHistoryBinding
import com.greencom.android.podcasts.ui.activity.ActivityFragmentDirections
import com.greencom.android.podcasts.ui.activity.history.ActivityHistoryViewModel.ActivityHistoryEvent
import com.greencom.android.podcasts.ui.activity.history.ActivityHistoryViewModel.ActivityHistoryState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        initRecyclerView()
        initObservers()
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

    /** Set observers for ViewModel observables. */
    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe UI states.
                launch {
                    viewModel.uiState.collectLatest { state ->
                        when (state) {
                            // Show Success screen.
                            is ActivityHistoryState.Success -> adapter.submitList(state.episodes)

                            // Show Empty screen.
                            ActivityHistoryState.Empty -> {  }
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
}