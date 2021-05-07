package com.greencom.android.podcasts.network

import com.greencom.android.podcasts.data.database.EpisodeEntity
import com.greencom.android.podcasts.data.database.PodcastEntityPartial
import com.greencom.android.podcasts.data.database.PodcastEntityPartialWithGenre

/** This file contains methods that convert data transfer objects to the database entities. */

/** Convert a [BestPodcastsWrapper] object to a [PodcastEntityPartialWithGenre] list. */
fun BestPodcastsWrapper.toDatabase(): List<PodcastEntityPartialWithGenre> = podcasts.map {
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

/** Convert a [PodcastWrapper] object to a [PodcastEntityPartial]. */
fun PodcastWrapper.podcastToDatabase(): PodcastEntityPartial = PodcastEntityPartial(
    id = id,
    title = title.trim(),
    description = description.trim(),
    image = image,
    publisher = publisher.trim(),
    explicitContent = explicitContent,
    episodeCount = episodeCount,
    latestPubDate = latestPubDate,
)

/** Convert a [PodcastWrapper.episodes] list to a [EpisodeEntity] list. */
fun PodcastWrapper.episodesToDatabase(): List<EpisodeEntity> = episodes.map {
    EpisodeEntity(
        id = it.id,
        title = it.title.trim(),
        description = it.description.trim(),
        image = it.image,
        audio = it.audio,
        audioLength = it.audioLength,
        podcastId = id,
        explicitContent = it.explicitContent,
        date = it.date
    )
}