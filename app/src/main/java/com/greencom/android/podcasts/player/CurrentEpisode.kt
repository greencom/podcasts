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
        fun empty(): CurrentEpisode = CurrentEpisode("", "", "", "")

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