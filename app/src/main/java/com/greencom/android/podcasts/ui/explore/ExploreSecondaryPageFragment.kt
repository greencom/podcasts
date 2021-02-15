package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.greencom.android.podcasts.databinding.FragmentExploreSecondaryPageBinding
import dagger.hilt.android.AndroidEntryPoint

// The fragment initialization parameter.
private const val GENRE_NAME = "genre_name"

/**
 * Represents all pages of the ViewPager2 in the ExploreFragment except the first one.
 * Each page contains a large list of podcasts for the specified genre.
 *
 * Use [ExploreSecondaryPageFragment.newInstance] to create a new instance
 * of the fragment using the provided parameters.
 */
@AndroidEntryPoint
class ExploreSecondaryPageFragment : Fragment() {

    /** The name of the genre associated with this fragment. */
    private lateinit var genreName: String

    /** Nullable View binding. Use [binding] instead. */
    private var _binding: FragmentExploreSecondaryPageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            genreName = it.getString(GENRE_NAME)!!
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
