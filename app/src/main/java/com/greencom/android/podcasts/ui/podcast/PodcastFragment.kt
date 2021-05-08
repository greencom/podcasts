package com.greencom.android.podcasts.ui.podcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.button.MaterialButton
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentPodcastBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastEvent
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastState
import com.greencom.android.podcasts.utils.setupSubscribeToggleButton
import com.greencom.android.podcasts.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class PodcastFragment : Fragment(), UnsubscribeDialog.UnsubscribeDialogListener {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentPodcastBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** PodcastViewModel. */
    private val viewModel: PodcastViewModel by viewModels()

    // Safe Args arguments.
    private val args: PodcastFragmentArgs by navArgs()

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
        // Get the podcast ID from the navigation arguments.
        val id = args.podcastId

        // TODO
        viewModel.getPodcast(id)

        // TODO
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is PodcastState.Success -> {
                        val podcast = state.podcast
                        binding.cover.load(podcast.image) {
                            transformations(RoundedCornersTransformation(
                                resources.getDimension(R.dimen.corner_radius_medium)
                            ))
                            crossfade(true)
                            placeholder(R.drawable.shape_placeholder)
                            error(R.drawable.shape_placeholder)
                        }
                        binding.title.text = podcast.title
                        binding.publisher.text = podcast.publisher
                        binding.description.text = podcast.description
                        binding.explicitContent.isVisible = podcast.explicitContent
                        binding.episodeCount.text = resources.getQuantityString(
                            R.plurals.podcast_episode_count,
                            podcast.episodeCount,
                            podcast.episodeCount
                        )
                        binding.toolbarTitle.text = podcast.title

                        // Setup `Subscribe` button.
                        setupSubscribeToggleButton(
                            binding.subscribe,
                            podcast.subscribed,
                            requireContext()
                        )

                        // Update subscription to the podcast.
                        binding.subscribe.setOnClickListener {
                            viewModel.updateSubscription(id, (it as MaterialButton).isChecked)
                            // Keep the button checked until the user makes his choice
                            // in the UnsubscribeDialog.
                            if (podcast.subscribed) binding.subscribe.isChecked = true
                        }
                    }
                }
            }
        }

        // TODO
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.event.collect { event ->
                when (event) {

                    // Show a snackbar.
                    is PodcastEvent.Snackbar -> showSnackbar(binding.root, event.stringRes)

                    // Show UnsubscribeDialog.
                    is PodcastEvent.UnsubscribeDialog ->
                        UnsubscribeDialog.show(childFragmentManager, id)
                }
            }
        }

        // Handle toolbar back button clicks.
        binding.toolbarBack.setOnClickListener {
            findNavController().navigateUp()
        }
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
}