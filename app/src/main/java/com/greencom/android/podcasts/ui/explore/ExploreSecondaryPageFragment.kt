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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.FragmentExploreSecondaryPageBinding
import com.greencom.android.podcasts.utils.CustomDividerItemDecoration
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

// Initialization parameters.
private const val GENRE_ID = "genre_id"

/**
 * Represents all pages of the ViewPager2 in the ExploreFragment except the first one.
 * Each page contains a list of best podcasts for the specified genre.
 *
 * Use [ExploreSecondaryPageFragment.newInstance] to create a new instance
 * of the fragment with provided parameters.
 */
@AndroidEntryPoint
class ExploreSecondaryPageFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentExploreSecondaryPageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by viewModels()

    /** RecyclerView adapter. */
    private val adapter = ExplorePodcastAdapter()

    // TODO
    private var podcastsJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentExploreSecondaryPageBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get the genre ID from the fragment arguments.
        val genreId = arguments?.getInt(GENRE_ID) ?: 0

        // RecyclerView setup.
        setupRecyclerView()
        // Swipe-to-refresh theming.
        customizeSwipeToRefresh()

        // TODO
        podcastsJob = lifecycleScope.launchWhenResumed {
            viewModel.getBestPodcasts(genreId).collectLatest { state ->
                binding.loading.isVisible = state is State.Loading
                binding.swipeToRefresh.isVisible = state is State.Success<*>
                binding.error.root.isVisible = state is State.Error

                @Suppress("UNCHECKED_CAST")
                if (state is State.Success<*>) {
                    adapter.submitList(state.data as List<Podcast>)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Cancel coroutine to stop collecting Flow with the best podcasts.
        podcastsJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
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
            adapter = this@ExploreSecondaryPageFragment.adapter
            addItemDecoration(divider)
        }
    }

    /** Swipe-to-refresh theming. */
    private fun customizeSwipeToRefresh() {
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * the fragment using the provided parameters.
         *
         * @param genreId ID of the genre.
         * @return A new instance of [ExploreSecondaryPageFragment].
         */
        @JvmStatic
        fun newInstance(genreId: Int) =
            ExploreSecondaryPageFragment().apply {
                arguments = Bundle().apply {
                    putInt(GENRE_ID, genreId)
                }
            }
    }
}