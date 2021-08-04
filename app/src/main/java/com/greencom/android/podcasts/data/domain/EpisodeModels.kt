package com.greencom.android.podcasts.data.domain

import androidx.room.ColumnInfo
import androidx.room.Ignore

/** Domain model class that represents an episode object. */
data class Episode @JvmOverloads constructor(

    /** Episode ID. */
    @ColumnInfo(name = "id")
    val id: String,

    /** Episode title. */
    @ColumnInfo(name = "title")
    val title: String,

    /** Episode description. */
    @ColumnInfo(name = "description")
    val description: String,

    /** Podcast title. */
    @ColumnInfo(name = "podcast_title")
    val podcastTitle: String,

    /** Podcast publisher. */
    @ColumnInfo(name = "publisher")
    val publisher: String,

    /** Image URL. */
    @ColumnInfo(name = "image")
    val image: String,

    /** Audio URL. */
    @ColumnInfo(name = "audio")
    val audio: String,

    /** Audio length in seconds. */
    @ColumnInfo(name = "audio_length")
    val audioLength: Int,

    /** The ID of the podcast that this episode belongs to. */
    @ColumnInfo(name = "podcast_id")
    val podcastId: String,

    /** Whether this podcast contains explicit language. */
    @ColumnInfo(name = "explicit_content")
    val explicitContent: Boolean,

    /** Published date in milliseconds. */
    @ColumnInfo(name = "date")
    val date: Long,

    /** The position where the episode was last stopped. */
    @ColumnInfo(name = "position")
    val position: Long,

    /** Whether the episode is completed. */
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,

    /**
     * Date when the episode was completed in milliseconds. `0`, if the episode is not completed.
     */
    @ColumnInfo(name = "completion_date")
    val completionDate: Long,

    /** Is this episode in the playlist. */
    @ColumnInfo(name = "in_playlist")
    val inPlaylist: Boolean,

    /**
     * Date in ms when this episode was added to the playlist. `0` if the episode is not
     * in the playlist.
     */
    @ColumnInfo(name = "added_to_playlist_date")
    val addedToPlaylistDate: Long,

    /** Whether the episode is selected by the user to play. */
    @Ignore
    val isSelected: Boolean = false,

    /** Whether the episode is playing right now. Makes sense only if the episode is selected. */
    @Ignore
    val isPlaying: Boolean = false,
)