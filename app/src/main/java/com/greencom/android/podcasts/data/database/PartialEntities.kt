package com.greencom.android.podcasts.data.database

import com.greencom.android.podcasts.data.domain.Podcast

/**
 * Model class that used to update entries in the `podcasts` table without
 * editing the [Podcast.inSubscriptions] property.
 */
data class PodcastEntityUpdateWithoutSubscription(

    /** Podcast ID. */
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
)

/**
 * Model class that used to update entries in the `podcasts` table by
 * editing ONLY the [Podcast.inSubscriptions] property.
 */
data class PodcastEntityUpdateSubscription(

    /** Podcast ID. */
    val id: String,

    /** Indicates whether the user is subscribed to this podcast. */
    val inSubscriptions: Boolean,
)