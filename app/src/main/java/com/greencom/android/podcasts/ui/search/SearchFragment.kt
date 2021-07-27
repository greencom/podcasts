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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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

    private val currentQuery: String
        get() = binding.search.text.toString().trim()

    private var nextOffset = 0

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

        hideScreens()
        viewModel.restoreLastSearch()

        if (viewModel.showKeyboard) {
            binding.search.requestFocus()
            showKeyboard(true)
            viewModel.showKeyboard = false
        }

        binding.error.tryAgain.setOnClickListener {
            search(currentQuery, 0)
        }

        binding.searchClear.setOnClickListener {
            binding.search.text.clear()
            binding.search.requestFocus()
            showKeyboard(true)
        }

        binding.appBarBack.setOnClickListener {
            showKeyboard(false)
            findNavController().navigateUp()
        }

        val onScrollListener = object : RecyclerView.OnScrollListener() {
            val layoutManager = binding.results.layoutManager as LinearLayoutManager
            var totalItemCount = 0
            var lastVisibleItemPosition = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                totalItemCount = layoutManager.itemCount
                lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (totalItemCount >= 10 &&
                    lastVisibleItemPosition >= totalItemCount - 1 &&
                    dy > 0 &&
                    nextOffset <= 20 // ListenAPI restrictions.
                ) {
                    search(viewModel.lastQuery, nextOffset)
                }
            }
        }

        binding.results.apply {
            adapter = this@SearchFragment.adapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            addOnScrollListener(onScrollListener)
            setOnTouchListener { _, _ ->
                showKeyboard(false)
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collectLatest { state ->
                        binding.progressBar.hideCrossfade()

                        when (state) {
                            SearchState.EmptyQuery -> hideScreens()

                            SearchState.Loading -> showLoadingScreen()

                            is SearchState.Success -> {
                                showSuccessScreen()
                                adapter.submitList(state.podcasts)
                                nextOffset = state.nextOffset
                                if (state.query != binding.search.text.toString()) {
                                    binding.search.setText(state.query)
                                }
                                if (viewModel.selectAll) {
                                    binding.search.selectAll()
                                }
                            }

                            SearchState.NothingFound -> {
                                adapter.submitList(emptyList())
                                delay(100)
                                showNothingFoundScreen()
                            }

                            is SearchState.Error -> {
                                adapter.submitList(emptyList())
                                delay(100)
                                showErrorScreen()
                            }
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

                            SearchEvent.StartProgressBar -> binding.progressBar.revealImmediately()

                            SearchEvent.CancelProgressBar -> binding.progressBar.hideCrossfade()
                        }
                    }
                }
            }
        }

        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(currentQuery, 0)
                return@setOnEditorActionListener true
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun search(query: String, offset: Int) {
        showKeyboard(false)
        viewModel.search(query, offset)
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
            nothingFound.root.hideImmediately()
        }
    }

    private fun showLoadingScreen() {
        binding.apply {
            results.hideImmediately()
            loading.revealImmediately()
            error.root.hideImmediately()
            nothingFound.root.hideImmediately()
        }
    }

    private fun showErrorScreen() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.revealCrossfade()
            nothingFound.root.hideImmediately()
        }
    }

    private fun showNothingFoundScreen() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.hideImmediately()
            nothingFound.root.revealCrossfade()
        }
    }

    private fun hideScreens() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.hideImmediately()
            nothingFound.root.hideImmediately()
        }
    }
}