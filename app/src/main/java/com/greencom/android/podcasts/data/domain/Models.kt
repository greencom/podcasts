package com.greencom.android.podcasts.data.domain

import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.database.PodcastEntityUpdateSubscription

/** Model class that represents a domain podcast object. */
data class Podcast(

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

    /** Indicates whether the user is subscribed to this podcast. */
    var inSubscriptions: Boolean,
) {

    /**
     * Convert a [Podcast] object to a [PodcastEntityUpdateSubscription] object
     * with `inSubscription` property set to a given value.
     *
     * [PodcastEntityUpdateSubscription] used as parameter for [PodcastDao.update] to
     * update the `inSubscription` property of the existing entry.
     */
    fun updateSubscription(newValue: Boolean): PodcastEntityUpdateSubscription {
        return PodcastEntityUpdateSubscription(
            id = this.id,
            inSubscriptions = newValue
        )
    }

    companion object {
        /**
         * Constant that means the podcast is not on the best list
         * for any genre.
         */
        const val NOT_IN_BEST = -1
    }
}

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

    /**
     * Parent genre ID.
     *
     * If the value is [Genre.NO_PARENT_GENRE], it means that this genre
     * does not have a parent genre.
     */
    val parentId: Int,
) {
    companion object {
        /** Constant that means the genre does not have a parent genre. */
        const val NO_PARENT_GENRE = -1
    }
}