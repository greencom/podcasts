package com.greencom.android.podcasts.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
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

        // Switch night mode
        binding.nightModeButton.setOnClickListener {
            when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_UNSPECIFIED ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                AppCompatDelegate.MODE_NIGHT_NO ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                AppCompatDelegate.MODE_NIGHT_YES ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear HomeFragment View binding.
        _binding = null
    }
}
