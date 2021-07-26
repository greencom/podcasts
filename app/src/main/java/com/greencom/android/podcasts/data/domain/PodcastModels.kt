package com.greencom.android.podcasts.data.domain

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.greencom.android.podcasts.data.database.EpisodeEntity

/** Domain model class that represents a podcast with a list of its episodes. */
data class PodcastWithEpisodes(
    @Embedded
    val podcast: Podcast,

    @Relation(
        entity = EpisodeEntity::class,
        parentColumn = "id",
        entityColumn = "podcast_id"
    )
    val episodes: List<Episode>
)

/**
 * Domain model class that represents a podcast object. Used in cases when all
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

    /** The published date of the oldest episode of this podcast in milliseconds. */
    @ColumnInfo(name = "earliest_pub_date")
    val earliestPubDate: Long,

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
 * Domain model class that represents a short podcast object. Used as items in
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

/** Domain model class that represents podcast search results. */
data class PodcastSearchResult(

    /** The number of search results in this page. */
    val count: Int,

    /** The total number of search results. */
    val total: Int,

    /**
     * Pass this value to the `offset` parameter of `searchPodcast()` to do
     * pagination of search results.
     */
    val nextOffset: Int,

    /** A list of search results. */
    val podcasts: List<Podcast>,
)