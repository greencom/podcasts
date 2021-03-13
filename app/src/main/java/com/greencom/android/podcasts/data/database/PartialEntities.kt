package com.greencom.android.podcasts.data.database

import androidx.room.ColumnInfo
import com.greencom.android.podcasts.data.domain.Podcast

/**
 * Model class that used to update entries in the `podcasts` table without
 * editing the [Podcast.inSubscriptions] property.
 */
data class PodcastEntityUpdate(

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

    /**
     * The genre ID for which the podcast is featured on the best list.
     *
     * If the value is [Podcast.NOT_IN_BEST], it means that the podcast
     * is not on the best list for any genre.
     */
    @ColumnInfo(name = "in_best_for_genre")
    val inBestForGenre: Int,
)

/**
 * Model class that used to update entries in the `podcasts` table by
 * editing ONLY the [Podcast.inSubscriptions] property.
 */
data class PodcastEntityUpdateSubscription(

    /** Podcast ID. */
    @ColumnInfo(name = "id")
    val id: String,

    /** Indicates whether the user is subscribed to this podcast. */
    @ColumnInfo(name = "in_subscriptions")
    val inSubscriptions: Boolean,
)