package com.greencom.android.podcasts.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greencom.android.podcasts.data.domain.Genre
import com.greencom.android.podcasts.data.domain.Podcast

/** Model class that represents a podcast entity in the database. */
@Entity(tableName = "podcasts")
data class PodcastEntity(

    /** Podcast ID. */
    @PrimaryKey
    val id: String,

    /** Podcast title. */
    val title: String,

    /** Podcast description. */
    val description: String,

    /** Image URL. */
    val image: String,

    /** Podcast publisher. */
    val publisher: String,

    /** Whether this podcast contains explicit language. */
    val explicitContent: Boolean,

    /** Total number of episodes in this podcast. */
    val episodeCount: Int,

    /** The published date of the latest episode of this podcast in milliseconds. */
    val latestPubDate: Long,

    /**
     * The genre ID for which the podcast is featured on the best list.
     *
     * If the value is [Podcast.NOT_IN_BEST], it means that the podcast
     * is not on the best list for any genre.
     */
    val inBestForGenre: Int,

    /** Indicates whether the user is subscribed to this podcast. */
    val inSubscriptions: Boolean,
)

/** Model class that represents a episode entity in the database. */
@Entity(tableName = "episodes")
data class EpisodeEntity(

    /** Episode ID. */
    @PrimaryKey
    val id: String,

    /** Episode title. */
    val title: String,

    /** Episode description. */
    val description: String,

    /** Image URL. */
    val image: String,

    /** Audio URL. */
    val audio: String,

    /** Audio length in seconds. */
    val audioLength: Int,

    /** The ID of the podcast that this episode belongs to. */
    val podcastId: String,

    /** Whether this podcast contains explicit language. */
    val explicitContent: Boolean,

    /** Published date in milliseconds. */
    val date: Long,
)



/** Model class that represents a genre entity in the database. */
@Entity(tableName = "genres")
data class GenreEntity(

    /** Genre ID. */
    @PrimaryKey
    val id: Int,

    /** Genre name. */
    val name: String,

    /**
     * Parent genre ID.
     *
     * If the value is [Genre.NO_PARENT_GENRE], it means
     * that this genre does not have a parent genre.
     */
    val parentId: Int,
)