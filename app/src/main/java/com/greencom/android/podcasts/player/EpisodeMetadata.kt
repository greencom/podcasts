package com.greencom.android.podcasts.player

import androidx.media2.common.MediaMetadata

/** Object that contains episode metadata keys. */
object EpisodeMetadata {

    /** Media metadata string key for episode ID. */
    const val ID = MediaMetadata.METADATA_KEY_MEDIA_ID

    /** Media metadata string key for episode title. */
    const val TITLE = MediaMetadata.METADATA_KEY_TITLE

    /** Media metadata string key for episode publisher. */
    const val PUBLISHER = MediaMetadata.METADATA_KEY_AUTHOR

    /** Media metadata string key for episode image URI. */
    const val IMAGE = MediaMetadata.METADATA_KEY_ART_URI

    /** Media metadata string key for episode duration. */
    const val DURATION = MediaMetadata.METADATA_KEY_DURATION
}