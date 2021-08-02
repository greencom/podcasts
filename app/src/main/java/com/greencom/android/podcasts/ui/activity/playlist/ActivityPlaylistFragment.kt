package com.greencom.android.podcasts.ui.activity.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greencom.android.podcasts.databinding.FragmentActivityPlaylistBinding
import dagger.hilt.android.AndroidEntryPoint

// TODO
@AndroidEntryPoint
class ActivityPlaylistFragment : Fragment() {

    private var _binding: FragmentActivityPlaylistBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}