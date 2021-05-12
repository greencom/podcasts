package com.greencom.android.podcasts.ui.podcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.button.MaterialButton
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.FragmentPodcastBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastEvent
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastState
import com.greencom.android.podcasts.utils.CustomDividerItemDecoration
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

    /** RecyclerView adapter. */
    private val adapter: PodcastEpisodeAdapter by lazy {
        PodcastEpisodeAdapter()
    }

    // TODO
    private lateinit var podcast: Podcast

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
        val divider = CustomDividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        divider.setDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.shape_divider, context?.theme)!!
        )
        binding.episodeList.apply {
            adapter = this@PodcastFragment.adapter
            addItemDecoration(divider)
        }

        // TODO
        val cl = binding.nestedScrollView.getChildAt(0) as ConstraintLayout
        val rv = cl.getChildAt(cl.childCount - 1)

        // TODO
        val onScrollChangeListener = NestedScrollView.OnScrollChangeListener {
                v, scrollX, scrollY, oldScrollX, oldScrollY ->

            if ((scrollY >= rv.measuredHeight - v.measuredHeight) && scrollY > oldScrollY) {
                if (adapter.currentList.isNotEmpty() && adapter.currentList.size != podcast.episodeCount) {
                    viewModel.fetchMoreEpisodes(id, adapter.currentList.last().date)
                }
            }
        }

        // TODO
        binding.nestedScrollView.setOnScrollChangeListener(onScrollChangeListener)

        // TODO
        viewModel.getPodcast(id)

        // TODO
        viewModel.fetchRecentEpisodes(id)

        // TODO
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is PodcastState.Success -> {
                        podcast = state.podcast
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

        // TODO
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.getEpisodes(id).collectLatest { episodes ->
                adapter.submitList(episodes)
            }
        }

        // Update subscription to the podcast.
        binding.subscribe.setOnClickListener {
            viewModel.updateSubscription(id, (it as MaterialButton).isChecked)
            // Keep the button checked until the user makes his choice
            // in the UnsubscribeDialog.
            if (podcast.subscribed) binding.subscribe.isChecked = true
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