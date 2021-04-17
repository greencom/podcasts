package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExplorePageBinding
import com.greencom.android.podcasts.ui.explore.ExploreViewModel.*
import com.greencom.android.podcasts.utils.CustomDividerItemDecoration
import com.greencom.android.podcasts.utils.REVEAL_ANIMATION_DURATION
import dagger.hilt.android.AndroidEntryPoint
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
class ExplorePageFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentExplorePageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by viewModels()

    /** RecyclerView adapter. */
    private val adapter = ExplorePodcastAdapter()

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

        // RecyclerView setup.
        setupRecyclerView()
        // Swipe-to-refresh setup.
        setupSwipeToRefresh()
        // Views alpha setup.
        setupAlpha()

        // Load the best podcasts.
        viewModel.getBestPodcasts(genreId)

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

        // Fetch the podcasts from the error screen.
        binding.error.tryAgain.setOnClickListener {
            viewModel.fetchBestPodcasts(genreId)
        }

        // Refresh the podcasts with swipe-to-refresh gesture.
        binding.swipeToRefresh.setOnRefreshListener {
            viewModel.refreshBestPodcasts(genreId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** Handle UI states. */
    private fun handleUiState(state: ExplorePageState) {
        binding.loading.isVisible = state is ExplorePageState.Loading
        binding.swipeToRefresh.isVisible = state is ExplorePageState.Success
        binding.error.root.isVisible = state is ExplorePageState.Error

        when (state) {
            is ExplorePageState.Success -> {
                adapter.submitList(state.podcasts)
                reveal(binding.podcastList)
                // Reset error screen alpha.
                binding.error.root.alpha = 0f
            }
            is ExplorePageState.Error -> {
                reveal(binding.error.root)
                // Reset podcast list alpha.
                binding.podcastList.alpha = 0f
            }
            is ExplorePageState.Loading -> {  }
        }
    }

    /** Handle events. */
    private fun handleEvent(event: ExplorePageEvent) {
        binding.swipeToRefresh.isRefreshing = event is ExplorePageEvent.Refreshing
        binding.error.tryAgain.isEnabled = event !is ExplorePageEvent.Fetching

        when (event) {
            is ExplorePageEvent.Snackbar -> showSnackbar(event.stringRes)
            is ExplorePageEvent.Fetching -> {  }
            is ExplorePageEvent.Refreshing -> {  }
        }
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
            val theme = activity?.theme
            theme?.resolveAttribute(
                R.attr.colorSwipeToRefreshBackground, backgroundColor, true
            )
            theme?.resolveAttribute(R.attr.colorPrimary, color, true)
            setProgressBackgroundColorSchemeColor(backgroundColor.data)
            setColorSchemeColors(color.data)
        }
    }

    /** Set up alpha of fragment views. Used to create crossfade animations on reveal. */
    private fun setupAlpha() {
        binding.error.root.alpha = 0f
        binding.podcastList.alpha = 0f
    }

    /** Reveal a view with crossfade animation. */
    private fun reveal(view: View) {
        view.animate()
            .alpha(1f)
            .setDuration(REVEAL_ANIMATION_DURATION)
    }

    /** Show a Snackbar with a message correspond to a given string res ID. */
    private fun showSnackbar(@StringRes stringRes: Int) {
        Snackbar.make(binding.root, stringRes, Snackbar.LENGTH_SHORT).show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * the fragment using the provided parameters.
         *
         * @param genreId ID of the genre.
         * @return A new instance of [ExplorePageFragment].
         */
        @JvmStatic
        fun newInstance(genreId: Int) =
            ExplorePageFragment().apply {
                arguments = Bundle().apply {
                    putInt(GENRE_ID, genreId)
                }
            }
    }
}