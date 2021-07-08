package com.greencom.android.podcasts.ui.episode

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.appbar.AppBarLayout
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.databinding.FragmentEpisodeBinding
import com.greencom.android.podcasts.ui.episode.EpisodeViewModel.EpisodeState
import com.greencom.android.podcasts.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

// Saving instance state.
private const val SAVED_STATE_IS_APP_BAR_EXPANDED = "IS_APP_BAR_EXPANDED"

// TODO
@AndroidEntryPoint
class EpisodeFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentEpisodeBinding? = null
    private val binding get() = _binding!!

    /** EpisodeViewModel. */
    private val viewModel: EpisodeViewModel by viewModels()

    // Navigation arguments.
    private val args: EpisodeFragmentArgs by navArgs()

    /** ID of the episode associated with this fragment. */
    private var episodeId = ""

    /** Episode associated with this fragment. */
    private var episode: Episode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentEpisodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @ExperimentalTime
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        binding.root.doOnPreDraw { startPostponedEnterTransition() }

        // Restore instance state.
        savedInstanceState?.apply {
            binding.appBarLayout.setExpanded(getBoolean(SAVED_STATE_IS_APP_BAR_EXPANDED), false)
        }

        // Get the episode ID from the navigation arguments.
        episodeId = args.episodeId

        // Load an episode.
        viewModel.getEpisode(episodeId)

        initAppBar()
        initViews()
        initObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean(SAVED_STATE_IS_APP_BAR_EXPANDED, viewModel.isAppBarExpanded.value)
        }
    }

    /** App bar setup. */
    private fun initAppBar() {
        // Disable AppBarLayout dragging behavior.
        setAppBarLayoutCanDrag(binding.appBarLayout, false)

        // Track app bar state.
        binding.appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, newState: Int) {
                when (newState) {
                    EXPANDED -> viewModel.setAppBarState(isExpanded = true)
                    COLLAPSED -> viewModel.setAppBarState(isExpanded = false)
                    else -> {  }
                }
            }
        })
    }

    /** Fragment views setup. */
    private fun initViews() {
        // Hide all screens to then reveal them with crossfade animations.
        hideScreens()

        // Set up error screen.
        binding.error.tlsTextView2.text = getString(R.string.episode_no_such_episode)
        binding.error.tryAgain.text = getString(R.string.episode_go_back)
        binding.error.tryAgain.setOnClickListener { findNavController().navigateUp() }

        // Handle toolbar back button clicks.
        binding.appBarBack.setOnClickListener { findNavController().navigateUp() }

        // Resume or pause depending on the current state or play if the episode is not selected.
        binding.play.setOnClickListener {
            episode?.let { episode ->
                if (episode.isSelected) {
                    if (episode.isPlaying) viewModel.pause() else viewModel.play()
                } else {
                    viewModel.playEpisode(episode.id)
                }
            }
        }

        // Show and hide app bar divider depending on the scroll state.
        binding.appBarDivider.hideImmediately()
        binding.nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 0) {
                binding.appBarDivider.revealImmediately()
            } else {
                binding.appBarDivider.hideCrossfade()
            }
        }
    }

    /** Set observers for ViewModel observables. */
    @ExperimentalTime
    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe UI states.
                launch {
                    viewModel.uiState.collectLatest { state ->
                        updateUi(state)
                    }
                }
            }
        }
    }

    /** Update UI. */
    @ExperimentalTime
    private fun updateUi(state: EpisodeState) {
        when (state) {
            // Show success screen.
            is EpisodeState.Success -> {
                showSuccessScreen()
                episode = state.episode
                val mEpisode = state.episode
                binding.cover.load(mEpisode.image) { coverBuilder(requireContext()) }
                binding.podcastTitle.text = mEpisode.podcastTitle
                binding.date.text = episodePubDateToString(mEpisode.date, requireContext())
                binding.episodeTitle.text = mEpisode.title
                binding.description.text = HtmlCompat.fromHtml(
                    mEpisode.description,
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                ).trim()
                binding.description.movementMethod = LinkMovementMethod.getInstance()
                setupPlayButton(binding.play, mEpisode, requireContext())
            }

            // Show error screen.
            is EpisodeState.Error -> showErrorScreen()

            // Show loading screen.
            is EpisodeState.Loading -> showLoadingScreen()
        }
    }

    /** Show success screen and hide all others. */
    private fun showSuccessScreen() {
        binding.nestedScrollView.revealCrossfade()
        binding.error.root.hideImmediately()
        binding.loading.hideImmediately()
    }

    /** Show loading screen and hide all others. */
    private fun showLoadingScreen() {
        binding.loading.revealImmediately()
        binding.nestedScrollView.hideImmediately()
        binding.error.root.hideImmediately()
    }

    /** Show error screen and hide all others. */
    private fun showErrorScreen() {
        binding.error.root.revealCrossfade()
        binding.nestedScrollView.hideImmediately()
        binding.loading.hideImmediately()
    }

    /** Hide all screens. */
    private fun hideScreens() {
        binding.nestedScrollView.hideImmediately()
        binding.error.root.hideImmediately()
        binding.loading.hideImmediately()
    }
}