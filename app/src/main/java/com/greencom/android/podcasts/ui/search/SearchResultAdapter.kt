package com.greencom.android.podcasts.ui.search

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.ItemSearchPodcastBinding
import com.greencom.android.podcasts.utils.PodcastDiffCallback
import com.greencom.android.podcasts.utils.coilCoverBuilder

/** Adapter used for RecyclerView that represents a list of podcast search results. */
class SearchResultAdapter(
    private val navigateToPodcast: (String) -> Unit,
) : ListAdapter<Podcast, SearchResultAdapter.ViewHolder>(PodcastDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(
            parent = parent,
            navigateToPodcast = navigateToPodcast
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast)
    }

    /** Represents a single podcast item in the list. */
    class ViewHolder private constructor(
        private val binding: ItemSearchPodcastBinding,
        private val navigateToPodcast: (String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context: Context
            get() = binding.root.context

        /** Podcast associated with this ViewHolder. */
        private lateinit var podcast: Podcast

        init {
            // Navigate to a podcast page.
            binding.root.setOnClickListener {
                navigateToPodcast(podcast.id)
            }
        }

        /** Bind ViewHolder with a given podcast. */
        fun bind(podcast: Podcast) {
            this.podcast = podcast

            binding.apply {
                cover.load(podcast.image) { coilCoverBuilder(context) }
                title.text = podcast.title
                publisher.text = podcast.publisher
                description.text = HtmlCompat.fromHtml(
                    podcast.description,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString().trim()
            }
        }

        companion object {
            /** Create a [ViewHolder]. */
            fun create(
                parent: ViewGroup,
                navigateToPodcast: (String) -> Unit,
            ): ViewHolder {
                val binding = ItemSearchPodcastBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(
                    binding = binding,
                    navigateToPodcast = navigateToPodcast
                )
            }
        }
    }
}