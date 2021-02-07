package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greencom.android.podcasts.databinding.FragmentExplorePrimaryPageBinding

private const val ARG_PARAM = "param"

/** TODO: Documentation */
class ExplorePrimaryPageFragment : Fragment() {

    private var param: String? = null

    /** Nullable View binding. Use [binding] instead. */
    private var _binding: FragmentExplorePrimaryPageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param = it.getString(ARG_PARAM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /** View binding setup. */
        _binding = FragmentExplorePrimaryPageBinding.inflate(inflater, container, false)

        binding.textView.text = param

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear View binding.
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param: String) =
            ExplorePrimaryPageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM, param)
                }
            }
    }
}
