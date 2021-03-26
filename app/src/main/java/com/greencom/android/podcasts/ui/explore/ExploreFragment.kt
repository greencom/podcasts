package com.greencom.android.podcasts.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentExploreBinding
import com.greencom.android.podcasts.utils.observeEvent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Contains lists of the best podcasts for different genres implemented as tabs for
 * the TabLayout and provides a way for searching.
 */
@AndroidEntryPoint
class ExploreFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentExploreBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** ExploreViewModel. */
    private val viewModel: ExploreViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TabLayout and ViewPager2 setup.
        setupTabLayout()
        // Create a single snackbar to show messages without overloading.
        val snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT)

        // Observe viewModel messages.
        viewModel.message.observeEvent(viewLifecycleOwner) { message ->
            snackbar.setText(message)
            snackbar.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** TabLayout and ViewPager2 setup. */
    private fun setupTabLayout() {
        val pagerAdapter = ExploreViewPagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        // Set up the tabs.
        val tabs = resources.getStringArray(R.array.explore_tabs)
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }
}