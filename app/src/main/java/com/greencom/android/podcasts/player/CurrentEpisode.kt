package com.greencom.android.podcasts.player

import androidx.media2.common.MediaItem
import kotlin.time.ExperimentalTime

// TODO
data class CurrentEpisode(
    val id: String,
    val title: String,
    val publisher: String,
    val image: String,
) {

    fun isEmpty(): Boolean = id.isBlank()

    fun isNotEmpty(): Boolean = !isEmpty()

    companion object {
        private const val EMPTY = ""

        fun empty(): CurrentEpisode = CurrentEpisode(EMPTY, EMPTY, EMPTY, EMPTY)

        @ExperimentalTime
        fun from(mediaItem: MediaItem?): CurrentEpisode {
            return CurrentEpisode(
                id = mediaItem?.metadata?.getString(EpisodeMetadata.ID) ?: EMPTY,
                title = mediaItem?.metadata?.getString(EpisodeMetadata.TITLE) ?: EMPTY,
                publisher = mediaItem?.metadata?.getString(EpisodeMetadata.PUBLISHER) ?: EMPTY,
                image = mediaItem?.metadata?.getString(EpisodeMetadata.IMAGE) ?: EMPTY,
            )
        }
    }
}