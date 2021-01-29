package com.greencom.android.podcasts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentHomeBinding
import com.greencom.android.podcasts.viewmodels.HomeViewModel

class HomeFragment : Fragment() {

    /** Nullable HomeFragment View binding. Use [binding] instead. */
    private var _binding: FragmentHomeBinding? = null
    /** Non-null HomeFragment View binding. */
    private val binding get() = _binding!!

    /** HomeViewModel. */
    private val viewModel: HomeViewModel by navGraphViewModels(R.id.nav_graph)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        /** HomeFragment View binding setup. */
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear HomeFragment View binding.
        _binding = null
    }
}
