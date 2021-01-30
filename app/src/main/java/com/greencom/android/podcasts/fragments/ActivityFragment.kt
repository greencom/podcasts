package com.greencom.android.podcasts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greencom.android.podcasts.databinding.FragmentActivityBinding

class ActivityFragment : Fragment() {

    /** Nullable ActivityFragment View binding. Use [binding] instead. */
    private var _binding: FragmentActivityBinding? = null
    /** Non-null ActivityFragment View binding. */
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /** ActivityFragment View binding setup. */
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear ActivityFragment View binding.
        _binding = null
    }
}
