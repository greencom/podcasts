package com.greencom.android.podcasts.data.domain

import com.greencom.android.podcasts.data.database.PodcastEntity

/** This file contains methods that convert model objects to the database entities. */

/**
 * Convert a list of [Podcast]s to a list of [PodcastEntity] items and edit their `genreId`
 * properties. Given `genreId` value will be applied to all podcasts on the list.
 */
fun List<Podcast>.toDatabase(genreId: Int): List<PodcastEntity> = map {
    PodcastEntity(
        id = it.id,
        title = it.title,
        description = it.description,
        image = it.image,
        publisher = it.publisher,
        explicitContent = it.explicitContent,
        episodeCount = it.episodeCount,
        latestPubDate = it.latestPubDate,
        subscribed = it.subscribed,
        genreId = genreId
    )
}