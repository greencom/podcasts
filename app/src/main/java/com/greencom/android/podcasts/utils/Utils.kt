package com.greencom.android.podcasts.utils

import android.content.Context
import androidx.recyclerview.widget.DiffUtil
import com.greencom.android.podcasts.data.database.GenreEntity
import com.greencom.android.podcasts.data.database.PodcastEntity
import com.greencom.android.podcasts.data.domain.Genre
import com.greencom.android.podcasts.data.domain.Podcast

/** Global tag for logging. */
const val GLOBAL_TAG = "GLOBAL_TAG"

/** Callback for calculating the diff between two non-null [Podcast]s in a list. */
object PodcastDiffCallback : DiffUtil.ItemCallback<Podcast>() {
    override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
        return oldItem == newItem
    }
}

/** Convert a [PodcastEntity] list to a list of [Podcast]s. */
fun List<PodcastEntity>.asPodcasts(): List<Podcast> {
    return map {
        Podcast(
            id = it.id,
            title = it.title,
            description = it.description,
            image = it.image,
            publisher = it.publisher,
            explicitContent = it.explicitContent,
            episodeCount = it.episodeCount,
            latestPubDate = it.latestPubDate,
            inBestForGenre = it.inBestForGenre,
            inSubscriptions = it.inSubscriptions,
        )
    }
}



/** Convert a [GenreEntity] list to a list of [Genre]s. */
fun List<GenreEntity>.asGenres(): List<Genre> {
    return map {
        Genre(
            id = it.id,
            name = it.name,
            parentId = it.parentId,
        )
    }
}

/** Convert `dp` to `px`. */
fun Context.dpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}

/** Convert `px` to `dp`. */
fun Context.pxToDp(px: Float): Float {
    return px / resources.displayMetrics.density
}