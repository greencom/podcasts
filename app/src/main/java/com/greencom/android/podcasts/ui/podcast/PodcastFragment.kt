package com.greencom.android.podcasts.ui.podcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.FragmentPodcastBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastEvent
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastState
import com.greencom.android.podcasts.utils.CustomDividerItemDecoration
import com.greencom.android.podcasts.utils.reveal
import com.greencom.android.podcasts.utils.setupSubscribeToggleButton
import com.greencom.android.podcasts.utils.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

    /** Podcast ID. */
    var id = ""

    /** The [Podcast] associated with this fragment. */
    private lateinit var podcast: Podcast

    /** RecyclerView adapter. */
    private val adapter: PodcastEpisodeAdapter by lazy {
        PodcastEpisodeAdapter()
    }

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

        // Postpone and start transition.
        postponeEnterTransition()
        binding.root.doOnPreDraw { startPostponedEnterTransition() }

        // Get the podcast ID from the navigation arguments.
        id = args.podcastId

        // Load the podcast.
        viewModel.getPodcast(id)

        // TODO
//        val cl = binding.nestedScrollView.getChildAt(0) as ConstraintLayout
//        val rv = cl.getChildAt(cl.childCount - 1)
//        val onScrollChangeListener = NestedScrollView.OnScrollChangeListener {
//                v, scrollX, scrollY, oldScrollX, oldScrollY ->
//
//            if ((scrollY >= rv.measuredHeight - v.measuredHeight) && scrollY > oldScrollY) {
//                if (adapter.currentList.isNotEmpty() && adapter.currentList.size != podcast.episodeCount) {
//                    viewModel.fetchMoreEpisodes(id, adapter.currentList.last().date)
//                }
//            }
//        }
//        binding.nestedScrollView.setOnScrollChangeListener(onScrollChangeListener)

        setupAppBar()
        setupRecyclerView()
        setupViews()

        setObservers()
        setOnTouchListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
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
    }

    /** RecyclerView setup. */
    private fun setupRecyclerView() {
        val divider = CustomDividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        divider.setDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.shape_divider, context?.theme)!!
        )
        binding.episodeList.apply {
            adapter = this@PodcastFragment.adapter
            addItemDecoration(divider)
        }
    }

    /** Set up fragment views. */
    private fun setupViews() {
        // Set alpha to create crossfade animations on reveal.
        binding.error.root.alpha = 0f
        binding.nestedScrollView.alpha = 0f
        binding.error.progressBar.alpha = 0f
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

        // Observe podcast episodes.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.getEpisodes(id).collectLatest { episodes ->
                adapter.submitList(episodes)
            }
        }
    }

    /** Set fragment's view on touch listeners. */
    private fun setOnTouchListeners() {
        // Update subscription to the podcast.
        binding.subscribe.setOnClickListener {
            viewModel.updateSubscription(id, (it as MaterialButton).isChecked)
            // Keep the button checked until the user makes his choice
            // in the UnsubscribeDialog.
            if (podcast.subscribed) binding.subscribe.isChecked = true
        }

        // Fetch the podcast from the error screen.
        binding.error.tryAgain.setOnClickListener {
            viewModel.fetchPodcast(id)
        }

        // Handle toolbar back button clicks.
        binding.toolbarBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    /** Handle UI states. */
    private fun handleUiState(state: PodcastState) {
        binding.nestedScrollView.isVisible = state is PodcastState.Success
        binding.error.root.isVisible = state is PodcastState.Error
        binding.loading.isVisible = state is PodcastState.Loading

        when (state) {
            // Show podcast data.
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

                binding.nestedScrollView.reveal()
                // Reset error screen alpha.
                binding.error.root.alpha = 0f
                // Reset "Try again" button text.
                binding.error.tryAgain.text = getString(R.string.explore_try_again)
                // Reset loading indicator alpha.
                binding.error.progressBar.alpha = 0f
            }

            // Show error screen.
            is PodcastState.Error -> {
                binding.error.root.reveal()
                // Reset podcast data alpha.
                binding.nestedScrollView.alpha = 0f
            }

            // Make `when` expression exhaustive.
            is PodcastState.Loading -> {  }
        }
    }

    /** Handle events. */
    private suspend fun handleEvent(event: PodcastEvent) {
        binding.error.tryAgain.isEnabled = event !is PodcastEvent.Fetching
        binding.error.progressBar.isVisible = event is PodcastEvent.Fetching

        when (event) {

            // Show a snackbar.
            is PodcastEvent.Snackbar -> {
                showSnackbar(binding.root, event.stringRes)

                // Reset loading indicator alpha.
                binding.error.progressBar.alpha = 0f

                // Reset "Try again" button text.
                delay(200) // Delay to avoid blinking.
                binding.error.tryAgain.text = getString(R.string.explore_try_again)
            }

            // Show UnsubscribeDialog.
            is PodcastEvent.UnsubscribeDialog ->
                UnsubscribeDialog.show(childFragmentManager, podcast.id)

            // Show Loading process.
            is PodcastEvent.Fetching -> {
                binding.error.progressBar.reveal()
                binding.error.tryAgain.text = getString(R.string.podcast_loading)
            }
        }
    }

    // Unsubscribe from the podcast if the user confirms in the UnsubscribeDialog.
    override fun onUnsubscribeClick(podcastId: String) {
        viewModel.unsubscribe(podcastId)
    }
}