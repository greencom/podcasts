package com.greencom.android.podcasts.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentActivityBinding
import com.greencom.android.podcasts.utils.setupMaterialSharedAxisTransitions
import dagger.hilt.android.AndroidEntryPoint

/** Contains a playlist and a listening history made with TabLayout and ViewPager2. */
@AndroidEntryPoint
class ActivityFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaterialSharedAxisTransitions()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        binding.root.doOnPreDraw { startPostponedEnterTransition() }

        initTabLayout()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** TabLayout and ViewPager2 setup. */
    private fun initTabLayout() {
        val tabs = resources.getStringArray(R.array.activity_tabs)

        binding.viewPager.adapter = ActivityViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = tabs.size - 1

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }
}