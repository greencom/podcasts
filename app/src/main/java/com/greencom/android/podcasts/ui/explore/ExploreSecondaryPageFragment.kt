package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greencom.android.podcasts.databinding.FragmentExploreSecondaryPageBinding

// The fragment initialization parameters.
private const val GENRE_NAME = "genre_name"

/**
 * Represents all pages of the ViewPager2 in the ExploreFragment except the first one.
 * Each page contains a large list of podcasts for the specified genre.
 *
 * Use [ExploreSecondaryPageFragment.newInstance] to create a new instance
 * of the fragment using the provided parameters.
 */
class ExploreSecondaryPageFragment : Fragment() {

    private var genreName: String? = null

    /** Nullable View binding. Use [binding] instead. */
    private var _binding: FragmentExploreSecondaryPageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            genreName = it.getString(GENRE_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /** View binding setup. */
        _binding = FragmentExploreSecondaryPageBinding.inflate(inflater, container, false)

        binding.textView.text = genreName

        return binding.root
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
         * @param genreName Name of the genre.
         *
         * @return A new instance of [ExploreSecondaryPageFragment].
         */
        @JvmStatic
        fun newInstance(genreName: String) =
            ExploreSecondaryPageFragment().apply {
                arguments = Bundle().apply {
                    putString(GENRE_NAME, genreName)
                }
            }
    }
}
