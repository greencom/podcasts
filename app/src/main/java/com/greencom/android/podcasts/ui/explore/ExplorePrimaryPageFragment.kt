package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExplorePrimaryPageBinding

/** TODO: Documentation */
class ExplorePrimaryPageFragment : Fragment() {

    /** Nullable View binding. Use [binding] instead. */
    private var _binding: FragmentExplorePrimaryPageBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /** View binding setup. */
        _binding = FragmentExplorePrimaryPageBinding.inflate(inflater, container, false)

        binding.textView.text = resources.getString(R.string.long_string)

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear View binding.
        _binding = null
    }
}
