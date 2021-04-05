package com.greencom.android.podcasts.network

import com.greencom.android.podcasts.data.database.PodcastEntityPartialWithGenre

/** This file contains methods that convert data transfer objects to the database entities. */

/** Convert a [BestPodcastsWrapper] object to a [PodcastEntityPartialWithGenre] list. */
fun BestPodcastsWrapper.asDatabaseEntities(): List<PodcastEntityPartialWithGenre> {
    return podcasts.map {
        PodcastEntityPartialWithGenre(
            id = it.id,
            title = it.title.trim(),
            description = it.description.trim(),
            image = it.image,
            publisher = it.publisher.trim(),
            explicitContent = it.explicitContent,
            episodeCount = it.episodeCount,
            latestPubDate = it.latestPubDate,
            genreId = this.genreId
        )
    }
}