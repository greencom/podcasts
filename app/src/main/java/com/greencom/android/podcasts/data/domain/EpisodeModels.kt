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

    /** The title of the parent podcast. */
    @ColumnInfo(name = "podcast_title")
    val podcastTitle: String,

    /** The publisher of the parent podcast. */
    @ColumnInfo(name = "publisher")
    val publisher: String,

    /** Image URL. */
    @ColumnInfo(name = "image")
    val image: String,

    /** Audio URL. */
    @ColumnInfo(name = "audio")
    val audio: String,

    /** Audio length in SECONDS. */
    @ColumnInfo(name = "audio_length")
    val audioLength: Int,

    /** The ID of parent podcast. */
    @ColumnInfo(name = "podcast_id")
    val podcastId: String,

    /** Whether this podcast contains explicit language. */
    @ColumnInfo(name = "explicit_content")
    val explicitContent: Boolean,

    /** The published date in milliseconds. */
    @ColumnInfo(name = "date")
    val date: Long,

    /** The position where the episode was stopped last time. */
    @ColumnInfo(name = "position")
    val position: Long,

    /** The date in ms when the episode was last time played. `0` if the episode was not played. */
    @ColumnInfo(name = "last_played_date")
    val lastPlayedDate: Long,

    /** Whether the episode is completed. */
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,

    /**
     * Date when the episode was completed in milliseconds. `0`, if the episode is not completed.
     */
    @ColumnInfo(name = "completion_date")
    val completionDate: Long,

    /** Is this episode in the bookmarks. */
    @ColumnInfo(name = "in_bookmarks")
    val inBookmarks: Boolean,

    /**
     * Date in ms when this episode was added to the bookmarks. `0` if the episode is not
     * in the bookmarks.
     */
    @ColumnInfo(name = "added_to_bookmarks_date")
    val addedToBookmarksDate: Long,

    /** Whether the episode is the current episode of the player. */
    @Ignore
    val isSelected: Boolean = false,

    /** Whether the episode is buffering right now. */
    @Ignore
    val isBuffering: Boolean = false,

    /** Whether the episode is playing right now. */
    @Ignore
    val isPlaying: Boolean = false,
)