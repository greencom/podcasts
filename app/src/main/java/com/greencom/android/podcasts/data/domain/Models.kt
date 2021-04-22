package com.greencom.android.podcasts.data.domain

import androidx.room.ColumnInfo

/**
 * Model class that represents a domain podcast object. Used in cases when all
 * podcast information needed.
 */
data class Podcast(

    /** Podcast ID. */
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
    @ColumnInfo(name = "subscribed")
    val subscribed: Boolean,
) {
    companion object {
        /** This podcast is not on any list of the best. */
        const val NO_GENRE_ID = -1
    }
}

/**
 * Model class that represents a short domain podcast object. Used as items in
 * different podcast lists.
 */
data class PodcastShort(

    /** Podcast ID. */
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

    /** Indicates whether the user is subscribed to this podcast. */
    @ColumnInfo(name = "subscribed")
    val subscribed: Boolean,

    /**
     * The ID of the genre for which this podcast is featured on the best list.
     * [Podcast.NO_GENRE_ID] by default, which means this podcast is not on any list of the best.
     */
    @ColumnInfo(name = "genre_id")
    val genreId: Int
)

/** Model class that represents a domain episode object. */
data class Episode(

    /** Episode ID. */
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

/** Model class that represents a domain genre object. */
data class Genre(

    /** Genre ID. */
    val id: Int,

    /** Genre name. */
    val name: String,

    /** Parent genre ID. */
    val parentId: Int,
)