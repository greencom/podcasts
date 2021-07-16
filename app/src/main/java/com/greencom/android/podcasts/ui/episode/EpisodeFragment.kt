package com.greencom.android.podcasts.ui.episode

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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
import com.greencom.android.podcasts.ui.episode.EpisodeViewModel.EpisodeEvent
import com.greencom.android.podcasts.ui.episode.EpisodeViewModel.EpisodeState
import com.greencom.android.podcasts.ui.podcast.PodcastFragment
import com.greencom.android.podcasts.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

// Saving instance state.
private const val SAVED_STATE_IS_APP_BAR_EXPANDED = "IS_APP_BAR_EXPANDED"

/** Regex pattern for detecting timecodes. */
private const val TIMECODE_PATTERN = "([0-9]{1,2})(:[0-9]{1,2}){1,2}"

/** Fragment that contains specific information about single episode. */
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMaterialSharedAxisTransitions(
            enter = true,
            exit = true,
            popEnter = true,
            popExit = true,
        )
    }

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
                when {
                    episode.isSelected && episode.isPlaying -> viewModel.pause()
                    episode.isSelected && !episode.isPlaying -> viewModel.play()
                    else -> viewModel.playEpisode(episode.id)
                }
            }
        }

        // Navigate to episode's parent podcast.
        binding.cover.setOnClickListener {
            viewModel.navigateToPodcast(episode?.podcastId)
        }
        binding.podcastTitle.setOnClickListener {
            viewModel.navigateToPodcast(episode?.podcastId)
        }
        binding.date.setOnClickListener {
            viewModel.navigateToPodcast(episode?.podcastId)
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

                // Observe events.
                launch {
                    viewModel.event.collect { event ->
                        handleEvent(event)
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
                val description = HtmlCompat.fromHtml(
                    mEpisode.description,
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )

                binding.apply {
                    cover.load(mEpisode.image) { coverBuilder(requireContext()) }
                    podcastTitle.text = mEpisode.podcastTitle
                    date.text = episodePubDateToString(mEpisode.date, requireContext())
                    episodeTitle.text = mEpisode.title
                    setupPlayButton(play, mEpisode, requireContext())
                    // Handle episode description.
                    binding.description.text = description.handleTimecodes().trim()
                    binding.description.movementMethod = LinkMovementMethod.getInstance()
                }
            }

            // Show error screen.
            is EpisodeState.Error -> showErrorScreen()

            // Show loading screen.
            is EpisodeState.Loading -> showLoadingScreen()
        }
    }

    /** Handle events. */
    @ExperimentalTime
    private fun handleEvent(event: EpisodeEvent) {
        when (event) {
            // Navigate to PodcastFragment.
            is EpisodeEvent.NavigateToPodcast -> {
                // Get the previous destination ID.
                val previousDestinationId =
                    findNavController().previousBackStackEntry?.destination?.id

                if (previousDestinationId == R.id.podcastFragment) {
                    // Previous destination is PodcastFragment, check destination args.
                    val previousPodcastId =
                        findNavController().previousBackStackEntry?.arguments?.getString(
                            PodcastFragment.SAFE_ARGS_PODCAST_ID
                        )
                    if (previousPodcastId == episode?.podcastId) {
                        // Previous PodcastFragment already contains the desired
                        // podcast, navigate up.
                        findNavController().navigateUp()
                    } else {
                        // Previous PodcastFragment DOES NOT contain the desired
                        // podcast, navigate to the appropriate PodcastFragment.
                        findNavController().navigate(
                            EpisodeFragmentDirections.actionEpisodeFragmentToPodcastFragment(event.podcastId)
                        )
                    }
                } else {
                    // Previous destination IS NOT PodcastFragment, navigate to
                    // PodcastFragment.
                    findNavController().navigate(
                        EpisodeFragmentDirections.actionEpisodeFragmentToPodcastFragment(event.podcastId)
                    )
                }
            }
        }
    }

    /**
     * Makes timecodes interactive. Works only for timecodes that follow [TIMECODE_PATTERN]
     * regex pattern.
     */
    @ExperimentalTime
    private fun Spanned.handleTimecodes(): SpannableStringBuilder {
        val spannableStringBuilder = SpannableStringBuilder(this)
        Regex(TIMECODE_PATTERN).findAll(this).forEach { timecode ->
            val startIndex = spannableStringBuilder.indexOf(timecode.value)
            val endIndex = startIndex + timecode.value.length
            val millis = obtainMillisFromTimecode(timecode.value)
            val spannableString = SpannableString(timecode.value)
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    viewModel.playFromTimecode(episodeId, millis)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }
            spannableString.setSpan(
                clickableSpan,
                0,
                timecode.value.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableStringBuilder.replace(startIndex, endIndex, spannableString)
        }
        return spannableStringBuilder
    }

    /** Obtains timestamp in milliseconds from the String timecode. */
    @ExperimentalTime
    private fun obtainMillisFromTimecode(timecode: String): Long {
        val timecodeParts = timecode.split(":")
        var hours = 0
        var minutes = 0
        var seconds = 0

        when (timecodeParts.size) {
            2 -> {
                minutes = timecodeParts[0].toInt()
                seconds = timecodeParts[1].toInt()
            }
            3 -> {
                hours = timecodeParts[0].toInt()
                minutes = timecodeParts[1].toInt()
                seconds = timecodeParts[2].toInt()
            }
        }
        val total = Duration.hours(hours) + Duration.minutes(minutes) + Duration.seconds(seconds)
        return total.inWholeMilliseconds
    }

    /** Show success screen and hide all others. */
    private fun showSuccessScreen() {
        binding.apply {
            nestedScrollView.revealCrossfade()
            error.root.hideImmediately()
            loading.hideImmediately()
        }
    }

    /** Show loading screen and hide all others. */
    private fun showLoadingScreen() {
        binding.apply {
            loading.revealImmediately()
            nestedScrollView.hideImmediately()
            error.root.hideImmediately()
        }
    }

    /** Show error screen and hide all others. */
    private fun showErrorScreen() {
        binding.apply {
            error.root.revealCrossfade()
            nestedScrollView.hideImmediately()
            loading.hideImmediately()
        }
    }

    /** Hide all screens. */
    private fun hideScreens() {
        binding.apply {
            nestedScrollView.hideImmediately()
            error.root.hideImmediately()
            loading.hideImmediately()
        }
    }

    companion object {

        /** Key to retrieve `episodeId` SafeArg from outside. */
        const val SAFE_ARGS_EPISODE_ID = "episodeId"
    }
}