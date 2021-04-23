package com.greencom.android.podcasts.data.database

import androidx.room.ColumnInfo
import com.greencom.android.podcasts.network.BestPodcastsWrapper
import com.greencom.android.podcasts.network.PodcastWrapper

/** This file contains helper entities that are partial to the main database entities. */

/**
 * Model class that represents a partial [PodcastEntity] without [PodcastEntity.subscribed]
 * and [PodcastEntity.genreId] properties.
 *
 * Used when converting DTOs such as [PodcastWrapper], which have only common podcast
 * fields. Consider using other helper entities when converting DTOs which have additional
 * fields, such as [PodcastEntityPartialWithGenre] for converting [BestPodcastsWrapper]
 * with [BestPodcastsWrapper.genreId] property.
 */
data class PodcastEntityPartial(

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
)

/**
 * Model class that represents a partial [PodcastEntity] without [PodcastEntity.subscribed]
 * property.
 *
 * Used when converting DTOs such as [BestPodcastsWrapper], which have additional
 * [BestPodcastsWrapper.genreId] property.
 */
data class PodcastEntityPartialWithGenre(

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

    /** The ID of the genre for which this podcast is featured on the best list. */
    @ColumnInfo(name = "genre_id")
    val genreId: Int
)

/**
 * Model class that represents a Podcast with subscription indicator. Used to update
 * subscription to a podcast by a podcast ID.
 */
data class PodcastSubscription(

    /** Podcast ID. */
    @ColumnInfo(name = "id")
    val id: String,

    /** Indicates whether the user is subscribed to this podcast. */
    @ColumnInfo(name = "subscribed")
    val subscribed: Boolean,
)