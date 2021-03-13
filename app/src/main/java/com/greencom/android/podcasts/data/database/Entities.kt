package com.greencom.android.podcasts.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greencom.android.podcasts.data.domain.Genre
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

    /**
     * The genre ID for which the podcast is featured on the best list.
     *
     * If the value is [Podcast.NOT_IN_BEST], it means that the podcast
     * is not on the best list for any genre.
     */
    @ColumnInfo(name = "in_best_for_genre")
    val inBestForGenre: Int,

    /** Indicates whether the user is subscribed to this podcast. */
    @ColumnInfo(name = "in_subscriptions")
    val inSubscriptions: Boolean,
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

    /**
     * Parent genre ID.
     *
     * If the value is [Genre.NO_PARENT_GENRE], it means
     * that this genre does not have a parent genre.
     */
    @ColumnInfo(name = "parent_id")
    val parentId: Int,
)