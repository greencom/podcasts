package com.greencom.android.podcasts.data.domain

import androidx.room.ColumnInfo

/** Domain model class that represents an episode object. */
data class Episode(

    /** Episode ID. */
    @ColumnInfo(name = "id")
    val id: String,

    /** Episode title. */
    @ColumnInfo(name = "title")
    val title: String,

    /** Episode description. */
    @ColumnInfo(name = "description")
    val description: String,

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

    // TODO
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,

    // TODO
    @ColumnInfo(name = "position")
    val position: Long,
)