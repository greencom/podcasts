package com.greencom.android.podcasts.ui.podcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import coil.transform.RoundedCornersTransformation
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.FragmentPodcastBinding
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

// TODO
@AndroidEntryPoint
class PodcastFragment : Fragment() {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentPodcastBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** PodcastViewModel. */
    private val viewModel: PodcastViewModel by viewModels()

    // Safe Args arguments.
    private val args: PodcastFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentPodcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get the podcast ID from the navigation arguments.
        val podcastId = args.podcastId

        // TODO
        viewModel.getPodcast(podcastId)

        // TODO
        lifecycleScope.launchWhenResumed {
            viewModel.podcast.collect { state ->
                if (state is State.Success<*>) {
                    val podcast = state.data as Podcast

                    binding.toolbarTitle.text = podcast.title

                    binding.cover.load(podcast.image) {
                        crossfade(true)
                        placeholder(R.drawable.shape_placeholder)
                        transformations(RoundedCornersTransformation(
                            resources.getDimension(R.dimen.corner_radius_small)
                        ))
                    }

                    binding.title.text = podcast.title
                    binding.publisher.text = podcast.publisher
                    binding.description.text = podcast.description
                    binding.episodeCount.text = resources.getQuantityString(
                        R.plurals.episode_count,
                        podcast.episodeCount,
                        podcast.episodeCount
                    )

                    binding.subscribe.isChecked = podcast.subscribed
                    updateSubscribeButton(podcast.subscribed)

                    binding.explicitContent.isVisible = podcast.explicitContent
                }
            }
        }

        // `Subscribe` button onClickListener.
        binding.subscribe.setOnClickListener {
            viewModel.updateSubscription(podcastId, binding.subscribe.isChecked)
        }

        // `Subscribe` button onCheckedChangeListener.
        binding.subscribe.addOnCheckedChangeListener { _, isChecked ->
            updateSubscribeButton(isChecked)
        }

        // Navigate up on back arrow click.
        binding.toolbarBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear View binding.
        _binding = null
    }

    /** Update the `Subscribe` button depending on whether it is checked. */
    private fun updateSubscribeButton(isChecked: Boolean) {
        if (isChecked) {
            binding.subscribe.icon = ResourcesCompat
                .getDrawable(resources, R.drawable.ic_check_24, context?.theme)
            binding.subscribe.text = resources.getString(R.string.explore_subscribed)
        } else {
            binding.subscribe.icon = ResourcesCompat
                .getDrawable(resources, R.drawable.ic_add_24, context?.theme)
            binding.subscribe.text = resources.getString(R.string.explore_subscribe)
        }
    }
}