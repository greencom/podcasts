package com.greencom.android.podcasts.player

import androidx.media2.common.MediaItem
import com.greencom.android.podcasts.player.MediaItemEpisode.Companion.empty
import com.greencom.android.podcasts.player.MediaItemEpisode.Companion.from
import kotlin.time.ExperimentalTime

/**
 * Wrapper class for Media2 media items. Use [from] to convert Media2 media items or [empty]
 * to create empty [MediaItemEpisode].
 */
data class MediaItemEpisode(
    val id: String,
    val title: String,
    val podcastTitle: String,
    val podcastId: String,
    val image: String,
) {

    /** Returns `true` if the episode is empty. Otherwise `false`. */
    fun isEmpty(): Boolean = id.isBlank()

    /** Returns `true` if the episode is NOT empty. Otherwise `false`. */
    fun isNotEmpty(): Boolean = !isEmpty()

    companion object {

        /** Create empty [MediaItemEpisode]. */
        fun empty(): MediaItemEpisode = MediaItemEpisode("", "", "", "", "")

        /**
         * Obtain [MediaItemEpisode] from the Media2 media item. If the media item is `null`,
         * returns empty [MediaItemEpisode].
         */
        @ExperimentalTime
        fun from(mediaItem: MediaItem?): MediaItemEpisode {
            return if (mediaItem == null) {
                MediaItemEpisode.empty()
            } else {
                MediaItemEpisode(
                    id = mediaItem.metadata?.getString(EpisodeMetadata.ID) ?: "",
                    title = mediaItem.metadata?.getString(EpisodeMetadata.TITLE) ?: "",
                    podcastTitle = mediaItem.metadata?.getString(EpisodeMetadata.PODCAST_TITLE) ?: "",
                    podcastId = mediaItem.metadata?.getString(EpisodeMetadata.PODCAST_ID) ?: "",
                    image = mediaItem.metadata?.getString(EpisodeMetadata.IMAGE) ?: "",
                )
            }
        }
    }
}