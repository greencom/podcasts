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

/** Fragment with podcast search. */
@AndroidEntryPoint
class SearchFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    /** SearchViewModel. */
    private val viewModel: SearchViewModel by viewModels()

    /** RecyclerView adapter. */
    private val adapter: SearchResultAdapter by lazy {
        SearchResultAdapter(
            navigateToPodcast = viewModel::navigateToPodcast
        )
    }

    /** String query that is currently set in the search field. */
    private val currentQuery: String
        get() = binding.search.text.toString().trim()

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
        // View binding setup.
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        binding.root.doOnPreDraw { startPostponedEnterTransition() }

        // Hide all screens at start and check for the last search.
        hideScreens()
        viewModel.restoreLastSearch()

        // Show keyboard if the fragment was just opened.
        if (viewModel.showKeyboard) {
            binding.search.requestFocus()
            showKeyboard(true)
            viewModel.showKeyboard = false
        }

        initViews()
        initRecyclerView()
        initObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** Fragment views setup. */
    private fun initViews() {
        // Handle toolbar back button clicks.
        binding.appBarBack.setOnClickListener {
            showKeyboard(false)
            findNavController().navigateUp()
        }

        // Repeat search from the error screen.
        binding.error.tryAgain.setOnClickListener {
            search(currentQuery, 0)
        }

        // Clear search query.
        binding.searchClear.setOnClickListener {
            binding.search.text.clear()
            binding.search.requestFocus()
            showKeyboard(true)
        }

        // Set up IME action to search.
        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(currentQuery, 0)
                return@setOnEditorActionListener true
            }
            false
        }
    }

    /** RecyclerView setup. */
    private fun initRecyclerView() {
        val onScrollListener = object : RecyclerView.OnScrollListener() {
            val layoutManager = binding.results.layoutManager as LinearLayoutManager
            var totalItemCount = 0
            var lastVisibleItemPosition = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                totalItemCount = layoutManager.itemCount
                lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // Load more search results on scroll.
                if (totalItemCount >= 10 &&
                    lastVisibleItemPosition >= totalItemCount - 1 &&
                    dy > 0 &&
                    viewModel.nextOffset <= 20 // ListenAPI restrictions.
                ) {
                    search(viewModel.lastQuery, viewModel.nextOffset)
                }
            }
        }

        binding.results.apply {
            adapter = this@SearchFragment.adapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            addOnScrollListener(onScrollListener)
            // Hide keyboard on touch.
            setOnTouchListener { _, _ ->
                showKeyboard(false)
                false
            }
        }
    }

    /** Set observers for ViewModel observables. */
    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe UI states.
                launch {
                    viewModel.uiState.collectLatest { state ->
                        binding.progressBar.hideCrossfade()

                        when (state) {
                            // Hide all screens when search() was called for an empty query.
                            SearchState.EmptyQuery -> hideScreens()

                            // Show loading screen.
                            SearchState.Loading -> showLoadingScreen()

                            // Show success screen.
                            is SearchState.Success -> {
                                showSuccessScreen()
                                adapter.submitList(state.podcasts)
                                // Set the restored result query to the search field.
                                if (state.query != binding.search.text.toString()) {
                                    binding.search.setText(state.query)
                                }
                                // Select whole query if the result is restored one.
                                if (viewModel.selectAll) {
                                    binding.search.selectAll()
                                }
                            }

                            // Show "Nothing found" screen.
                            SearchState.NothingFound -> {
                                adapter.submitList(emptyList())
                                delay(100) // Give adapter some time to set an empty list.
                                showNothingFoundScreen()
                            }

                            // Show Error screen.
                            is SearchState.Error -> {
                                adapter.submitList(emptyList())
                                delay(100) // Give adapter some time to set an empty list.
                                showErrorScreen()
                            }
                        }
                    }
                }

                // Observe events.
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            // Navigate to a podcast page.
                            is SearchEvent.NavigateToPodcast -> {
                                showKeyboard(false)
                                findNavController().navigate(
                                    SearchFragmentDirections.actionSearchFragmentToPodcastFragment(
                                        event.podcastId
                                    )
                                )
                            }

                            // Start LinearProgressBar animation.
                            SearchEvent.StartProgressBar -> binding.progressBar.revealImmediately()

                            // Cancel LinearProgressBar animation.
                            SearchEvent.CancelProgressBar -> binding.progressBar.hideCrossfade()
                        }
                    }
                }
            }
        }
    }

    /** Start search for given query and offset. */
    private fun search(query: String, offset: Int) {
        showKeyboard(false)
        viewModel.search(query, offset)
    }

    /** Show or hide keyboard. */
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

    /** Show Success screen. */
    private fun showSuccessScreen() {
        binding.apply {
            results.revealCrossfade()
            loading.hideImmediately()
            error.root.hideImmediately()
            nothingFound.root.hideImmediately()
        }
    }

    /** Show Loading screen. */
    private fun showLoadingScreen() {
        binding.apply {
            results.hideImmediately()
            loading.revealImmediately()
            error.root.hideImmediately()
            nothingFound.root.hideImmediately()
        }
    }

    /** Show Error screen. */
    private fun showErrorScreen() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.revealCrossfade()
            nothingFound.root.hideImmediately()
        }
    }

    /** Show "Nothing found" screen. */
    private fun showNothingFoundScreen() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.hideImmediately()
            nothingFound.root.revealCrossfade()
        }
    }

    /** Hide all screens. */
    private fun hideScreens() {
        binding.apply {
            results.hideImmediately()
            loading.hideImmediately()
            error.root.hideImmediately()
            nothingFound.root.hideImmediately()
        }
    }
}