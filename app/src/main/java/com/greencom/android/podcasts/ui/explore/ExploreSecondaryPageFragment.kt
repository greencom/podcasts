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
import kotlinx.coroutines.flow.collectLatest

// Initialization parameters.
private const val GENRE_ID = "genre_id"

/**
 * Represents all pages of the ViewPager2 in the ExploreFragment except the first one.
 * Each page contains a large list of podcasts for the specified genre.
 *
 * Use [ExploreSecondaryPageFragment.newInstance] to create a new instance
 * of the fragment using the provided parameters.
 */
@Suppress("UNCHECKED_CAST")
@AndroidEntryPoint
class ExploreSecondaryPageFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentExploreSecondaryPageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by activityViewModels()

    /** The ID of the genre associated with this fragment. */
    private var genreId = 0

    /** RecyclerView adapter. */
    private val adapter by lazy {
        ExplorePodcastAdapter(viewModel::updateSubscription)
    }

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
        _binding = FragmentExploreSecondaryPageBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        customizeSwipeToRefresh()

        /** TODO: Documentation */
        lifecycleScope.launchWhenResumed {
            viewModel.getBestPodcasts(genreId).collectLatest { state ->
                binding.swipeToRefresh.isVisible = state is State.Success<*>
                binding.loading.isVisible = state is State.Loading
                binding.error.root.isVisible = state is State.Error

                if (state is State.Success<*>) {
                    adapter.submitList(state.data as List<Podcast>)
                }
            }
        }

        // Update best podcasts from error screen.
        binding.error.tryAgain.setOnClickListener {
            viewModel.getBestPodcasts(genreId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear View binding.
        _binding = null
    }

    /** RecyclerView setup. */
    private fun setupRecyclerView() {
        binding.podcastList.apply {
            setHasFixedSize(true)
            adapter = this@ExploreSecondaryPageFragment.adapter
            val divider = CustomDividerItemDecoration(context, RecyclerView.VERTICAL)
            divider.setDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.shape_divider,
                    context?.theme
                )!!
            )
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