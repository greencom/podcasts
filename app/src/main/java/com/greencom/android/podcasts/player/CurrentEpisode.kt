package com.greencom.android.podcasts.player

import androidx.media2.common.MediaItem
import kotlin.time.ExperimentalTime

/** Wrapper class for the player media items. */
data class CurrentEpisode(
    val id: String,
    val title: String,
    val publisher: String,
    val image: String,
) {

    /** Returns `true` if the episode is empty. Otherwise `false`. */
    fun isEmpty(): Boolean = id.isBlank()

    /** Returns `true` if the episode is NOT empty. Otherwise `false`. */
    fun isNotEmpty(): Boolean = !isEmpty()

    companion object {

        /** Create an empty [CurrentEpisode]. */
        fun empty(): CurrentEpisode = CurrentEpisode("", "", "", "")

        /**
         * Obtain [CurrentEpisode] from the player media item. If the media item is `null`,
         * returns empty [CurrentEpisode].
         */
        @ExperimentalTime
        fun from(mediaItem: MediaItem?): CurrentEpisode {
            return if (mediaItem == null) {
                CurrentEpisode.empty()
            } else {
                CurrentEpisode(
                    id = mediaItem.metadata?.getString(EpisodeMetadata.ID) ?: "",
                    title = mediaItem.metadata?.getString(EpisodeMetadata.TITLE) ?: "",
                    publisher = mediaItem.metadata?.getString(EpisodeMetadata.PUBLISHER) ?: "",
                    image = mediaItem.metadata?.getString(EpisodeMetadata.IMAGE) ?: "",
                )
            }
        }
    }
}