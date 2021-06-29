package com.greencom.android.podcasts.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Model class that represents an episode entity in the database. */
@Entity(tableName = "episodes")
data class EpisodeEntity(

    /** Episode ID. */
    @PrimaryKey
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

    /** Whether this episode contains explicit language. */
    @ColumnInfo(name = "explicit_content")
    val explicitContent: Boolean,

    /** Published date in milliseconds. */
    @ColumnInfo(name = "date")
    val date: Long,

    /** Whether the episode is completed. */
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,

    /** The position where the episode was last stopped. */
    @ColumnInfo(name = "position")
    val position: Long,
)

/** Model class that used to update episode state in the database. */
data class EpisodeEntityState(

    /** Episode ID. */
    @ColumnInfo(name = "id")
    val id: String,

    /** The position where the episode was last stopped. */
    @ColumnInfo(name = "position")
    val position: Long,

    /** Whether the episode is completed. */
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
)