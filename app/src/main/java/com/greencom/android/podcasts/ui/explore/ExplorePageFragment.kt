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
import com.greencom.android.podcasts.databinding.FragmentExplorePageBinding
import com.greencom.android.podcasts.utils.CustomDividerItemDecoration
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

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

    /** Use this [Job] to control the state of the coroutine collecting the best podcasts. */
    private var podcastsJob: Job? = null

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
        setupSwipeToRefresh(genreId)

        // Collect the best podcasts. Launch the coroutine when the state becomes RESUMED and
        // cancel in onPause().
        podcastsJob = lifecycleScope.launchWhenResumed {
            viewModel.getBestPodcasts(genreId).collectLatest { state ->
                handleState(state)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel the coroutine to stop collecting the Flow with the best podcasts.
        podcastsJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear View binding.
        _binding = null
    }

    /** Handle the UI depending on [State]. */
    private fun handleState(state: State) {
        binding.loading.isVisible = state is State.Loading
        binding.swipeToRefresh.isVisible = state is State.Success<*>
        binding.error.root.isVisible = state is State.Error

        if (state is State.Success<*>) {
            @Suppress("UNCHECKED_CAST")
            adapter.submitList(state.data as List<Podcast>)
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

    /**
     * Swipe-to-refresh setup. Disable swipe-to-refresh if the page belongs to the
     * [ExploreTabGenre.MAIN], i.e. [genreId] is equal to `0`.
     */
    private fun setupSwipeToRefresh(genreId: Int) {
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

            // Disable swipe-to-refresh, if the genre ID is `0`.
            if (genreId == ExploreTabGenre.MAIN.id) {
                binding.swipeToRefresh.isEnabled = false
            }
        }
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