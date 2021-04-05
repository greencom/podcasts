package com.greencom.android.podcasts.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greencom.android.podcasts.data.domain.Podcast

/** Model class that represents a podcast entity in the database. */
@Entity(tableName = "podcasts")
data class PodcastEntity(

    /** Podcast ID. */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** Podcast title. */
    @ColumnInfo(name = "title")
    val title: String,

    /** Podcast description. */
    @ColumnInfo(name = "description")
    val description: String,

    /** Image URL. */
    @ColumnInfo(name = "image")
    val image: String,

    /** Podcast publisher. */
    @ColumnInfo(name = "publisher")
    val publisher: String,

    /** Whether this podcast contains explicit language. */
    @ColumnInfo(name = "explicit_content")
    val explicitContent: Boolean,

    /** Total number of episodes in this podcast. */
    @ColumnInfo(name = "episode_count")
    val episodeCount: Int,

    /** The published date of the latest episode of this podcast in milliseconds. */
    @ColumnInfo(name = "latest_pub_date")
    val latestPubDate: Long,

    /** Indicates whether the user is subscribed to this podcast. */
    @ColumnInfo(name = "subscribed", defaultValue = "0")
    val subscribed: Boolean,

    /**
     * The ID of the genre for which this podcast is featured on the best list.
     * [Podcast.NO_GENRE_ID] by default, which means this podcast is not on any list of the best.
     */
    @ColumnInfo(name = "genre_id", defaultValue = "-1")
    val genreId: Int
)

/** Model class that represents a episode entity in the database. */
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
)

/** Model class that represents a genre entity in the database. */
@Entity(tableName = "genres")
data class GenreEntity(

    /** Genre ID. */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    /** Genre name. */
    @ColumnInfo(name = "name")
    val name: String,

    /** Parent genre ID. */
    @ColumnInfo(name = "parent_id")
    val parentId: Int,
)