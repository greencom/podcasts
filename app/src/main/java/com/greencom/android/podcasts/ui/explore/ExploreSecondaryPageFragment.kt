package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.FragmentExploreSecondaryPageBinding
import com.greencom.android.podcasts.utils.CustomDividerItemDecoration
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val viewModel: ExploreViewModel by activityViewModels()

    /** RecyclerView adapter. */
    private val adapter by lazy {
        ExplorePodcastAdapter(viewModel::updateSubscription, viewModel::onPodcastClick)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentExploreSecondaryPageBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /** The ID of the genre associated with this fragment. */
        val genreId = arguments?.getInt(GENRE_ID) ?: 0

        // RecyclerView setup.
        setupRecyclerView()
        // Swipe-to-refresh theming.
        customizeSwipeToRefresh()

        // Get the best podcasts and display the result.
        lifecycleScope.launchWhenResumed {
            viewModel.getBestPodcasts(genreId).collectLatest { state ->
                binding.swipeToRefresh.isVisible = state is State.Success<*>
                binding.loading.isVisible = state is State.Loading
                binding.error.root.isVisible = state is State.Error

                if (state is State.Success<*>) {
                    @Suppress("UNCHECKED_CAST")
                    adapter.submitList(state.data as List<Podcast>)
                }
            }
        }

        // TODO: FIX
        // viewModel.updateBestPodcasts() always set a new value the to viewModel.message,
        // so observe it to cancel the refreshing animation.
        viewModel.message.observe(viewLifecycleOwner) {
            binding.swipeToRefresh.isRefreshing = false
            binding.podcastList.smoothScrollToPosition(0)
        }

        // Update the best podcasts from the error screen.
        binding.error.tryAgain.setOnClickListener {
            viewModel.fetchBestPodcasts(genreId)
        }

        // Update the best podcasts with swipe-to-refresh.
        binding.swipeToRefresh.setOnRefreshListener {
            viewModel.updateBestPodcasts(genreId)
        }
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
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.shape_divider,
                context?.theme
            )!!
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
            theme?.resolveAttribute(R.attr.colorSwipeToRefreshBackground, color, true)
            theme?.resolveAttribute(R.attr.colorPrimary, backgroundColor, true)
            setProgressBackgroundColorSchemeColor(color.data)
            setColorSchemeColors(backgroundColor.data)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * the fragment using the provided parameters.
         *
         * @param genreId ID of the genre.
         *
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