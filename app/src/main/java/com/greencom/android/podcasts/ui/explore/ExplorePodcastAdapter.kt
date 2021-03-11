package com.greencom.android.podcasts.ui.explore

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.PodcastItemBinding

/** TODO: Documentation */
class ExplorePodcastAdapter :
    ListAdapter<Podcast, ExplorePodcastViewHolder>(ExplorePodcastDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExplorePodcastViewHolder {
        return ExplorePodcastViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ExplorePodcastViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

/** TODO: Documentation */
class ExplorePodcastViewHolder private constructor(private val binding: PodcastItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val context: Context = binding.root.context

    /** TODO: Documentation */
    fun bind(item: Podcast) {
        Glide.with(binding.cover.context)
            .load(item.image)
            .into(binding.cover)
        binding.title.text = item.title
        binding.publisher.text = item.publisher
        binding.description.text =
            HtmlCompat.fromHtml(item.description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        if (item.inSubscriptions) {
            binding.subscribe.isChecked = true
            binding.subscribe.icon = ContextCompat.getDrawable(context, R.drawable.ic_check_24)
            binding.subscribe.text = context.getString(R.string.subscribed)
        }
        binding.explicitContent.isVisible = item.explicitContent

        setListeners()
    }

    /** TODO: Documentation */
    private fun setListeners() {
        // `Subscribe` button onClickListener.
        binding.subscribe.setOnClickListener {

        }

        // `Subscribe` button onCheckedChangeListener.
        binding.subscribe.addOnCheckedChangeListener { button, isChecked ->
            if (isChecked) {
                button.icon = ContextCompat.getDrawable(context, R.drawable.ic_check_24)
                button.text = context.getString(R.string.subscribed)
            } else {
                button.icon = ContextCompat.getDrawable(context, R.drawable.ic_add_24)
                button.text = context.getString(R.string.subscribe)
            }
        }
    }

    companion object {
        /** TODO: Documentation */
        fun create(parent: ViewGroup): ExplorePodcastViewHolder {
            val binding = PodcastItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return ExplorePodcastViewHolder(binding)
        }
    }
}

class ExplorePodcastDiffCallback : DiffUtil.ItemCallback<Podcast>() {
    override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem == newItem
    }
}