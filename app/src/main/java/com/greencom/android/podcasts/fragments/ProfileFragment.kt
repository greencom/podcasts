package com.greencom.android.podcasts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greencom.android.podcasts.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    /** Nullable ProfileFragment View binding. Use [binding] instead. */
    private var _binding: FragmentProfileBinding? = null
    /** Non-null ProfileFragment View binding. */
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /** ProfileFragment View binding setup. */
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear ProfileFragment View binding.
        _binding = null
    }
}
