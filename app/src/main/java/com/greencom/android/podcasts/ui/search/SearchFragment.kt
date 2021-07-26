package com.greencom.android.podcasts.ui.search

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialContainerTransform
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentSearchBinding
import com.greencom.android.podcasts.ui.search.SearchViewModel.SearchEvent
import com.greencom.android.podcasts.ui.search.SearchViewModel.SearchState
import com.greencom.android.podcasts.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// TODO
@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()

    private val adapter: SearchResultAdapter by lazy {
        SearchResultAdapter(
            navigateToPodcast = viewModel::navigateToPodcast
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaterialFadeThroughTransitions(
            enter = true,
            popExit = true,
        )
        setupMaterialSharedAxisTransitions(
            exit = true,
            popEnter = true
        )
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.shared_axis_transition_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        binding.root.doOnPreDraw { startPostponedEnterTransition() }

        // TODO: Check for the previous result.
        showQueryIsEmptyScreen()
        viewModel.checkLastSearch()

        if (viewModel.showKeyboard) {
            binding.search.requestFocus()
            showKeyboard(true)
            viewModel.selectAll = true
            viewModel.showKeyboard = false
        } else {
            viewModel.selectAll = false
        }

        binding.error.tryAgain.setOnClickListener {
            search()
        }

        binding.appBarBack.setOnClickListener {
            showKeyboard(false)
            findNavController().navigateUp()
        }

        binding.results.apply {
            adapter = this@SearchFragment.adapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collectLatest { state ->
                        binding.results.isVisible = state is SearchState.Success

                        when (state) {
                            SearchState.QueryIsEmpty -> showQueryIsEmptyScreen()

                            is SearchState.LastSearch -> {
                                showSuccessScreen()
                                binding.search.setText(state.query)
                                adapter.submitList(state.podcasts)
                                if (viewModel.selectAll) {
                                    binding.search.selectAll()
                                }
                            }

                            SearchState.Loading -> showLoadingScreen()

                            is SearchState.Success -> {
                                adapter.submitList(state.podcasts)
                                delay(200)
                                showSuccessScreen()
                            }

                            is SearchState.Error -> showErrorScreen()

                            SearchState.NoResultsFound -> showNoResultsFoundScreen()
                        }
                    }
                }

                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is SearchEvent.NavigateToPodcast -> {
                                showKeyboard(false)
                                findNavController().navigate(
                                    SearchFragmentDirections.actionSearchFragmentToPodcastFragment(
                                        event.podcastId
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun search() {
        viewModel.search(binding.search.text.toString().trim(), false, 0)
        showKeyboard(false)
    }

    private fun showKeyboard(show: Boolean) {
        val inputManager =
            activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            inputManager.showSoftInput(binding.search, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.search.clearFocus()
            inputManager.hideSoftInputFromWindow(binding.search.windowToken, 0)
        }
    }

    private fun showSuccessScreen() {
        binding.apply {
            results.revealCrossfade()
            loading.hideImmediately()
            error.root.hideImmediately()
            noResultsFound.hideImmediately()
            queryIsEmpty.hideImmediately()
        }
    }

    private fun showLoadingScreen() {
        binding.apply {
            results.hideImmediately()
            loading.revealImmediately()
            error.root.hideImmediately()
            noResultsFound.hideImmediately()
            queryIsEmpty.hideImmediately()
        }
    }

    private fun showErrorScreen() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.revealCrossfade()
            noResultsFound.hideImmediately()
            queryIsEmpty.hideImmediately()
        }
    }

    private fun showNoResultsFoundScreen() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.hideImmediately()
            noResultsFound.revealCrossfade()
            queryIsEmpty.hideImmediately()
        }
    }

    private fun showQueryIsEmptyScreen() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.hideImmediately()
            noResultsFound.hideImmediately()
            queryIsEmpty.revealCrossfade()
        }
    }

    private fun hideScreens() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.hideImmediately()
            noResultsFound.hideImmediately()
            queryIsEmpty.hideImmediately()
        }
    }
}