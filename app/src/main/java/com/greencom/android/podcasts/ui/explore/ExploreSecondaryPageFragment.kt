package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExploreSecondaryPageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Initialization parameters.
private const val GENRE_ID = "genre_id"

/**
 * Represents all pages of the ViewPager2 in the ExploreFragment except the first one.
 * Each page contains a large list of podcasts for the specified genre.
 *
 * Use [ExploreSecondaryPageFragment.newInstance] to create a new instance
 * of the fragment using the provided parameters.
 */
@AndroidEntryPoint
class ExploreSecondaryPageFragment private constructor(): Fragment() {

    /** The ID of the genre associated with this fragment. */
    private var genreId = 0

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentExploreSecondaryPageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by activityViewModels()

    /** RecyclerView adapter. */
    private val adapter = ExplorePodcastAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            genreId = it.getInt(GENRE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentExploreSecondaryPageBinding.inflate(inflater, container, false)

        // Adapter setup.
        binding.podcastList.adapter = adapter

        // Swipe-to-refresh theming.
        binding.refresh.apply {
            val color = TypedValue()
            val backgroundColor = TypedValue()
            val theme = activity?.theme
            theme?.resolveAttribute(R.attr.colorSwipeToRefreshBackground, color, true)
            theme?.resolveAttribute(R.attr.colorPrimary, backgroundColor, true)

            setProgressBackgroundColorSchemeColor(color.data)
            setColorSchemeColors(backgroundColor.data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /** TODO: Documentation */
        lifecycleScope.launch {
            viewModel.getBestPodcasts(genreId).collectLatest {
                adapter.submitData(it)
            }
        }

        /** TODO: Documentation */
        adapter.addLoadStateListener { loadState ->

        }

        /** TODO: Documentation */
        binding.refresh.setOnRefreshListener {
            adapter.refresh()
            lifecycleScope.launch {
                delay(750)
                binding.refresh.isRefreshing = false
            }
        }

        /** TODO: Documentation */

    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear View binding.
        _binding = null
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
