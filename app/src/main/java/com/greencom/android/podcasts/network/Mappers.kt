package com.greencom.android.podcasts.network

import com.greencom.android.podcasts.data.database.EpisodeEntity
import com.greencom.android.podcasts.data.database.PodcastEntityPartial
import com.greencom.android.podcasts.data.database.PodcastEntityPartialWithGenre
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.data.domain.PodcastSearchResult

/**
 * This file contains methods that convert data transfer objects to database or domain entities.
 */

/** Convert a [SearchPodcastWrapper] object to a [PodcastSearchResult] domain model object. */
fun SearchPodcastWrapper.toDomain(query: String, offset: Int): PodcastSearchResult = PodcastSearchResult(
    query = query,
    count = count,
    total = total,
    offset = offset,
    nextOffset = nextOffset,
    podcasts = podcasts.map { podcast ->
        Podcast(
            id = podcast.id,
            title = podcast.title.trim(),
            description = podcast.description.trim(),
            image = podcast.image,
            publisher = podcast.publisher.trim(),
            explicitContent = podcast.explicitContent,
            episodeCount = podcast.episodeCount,
            latestPubDate = podcast.latestPubDate,
            earliestPubDate = podcast.earliestPubDate,
            subscribed = false
        )
    }
)

/** Convert a [SearchPodcastWrapper] object to a list of [PodcastEntityPartial]. */
fun SearchPodcastWrapper.podcastsToDatabase(): List<PodcastEntityPartial> = podcasts.map { podcast ->
    PodcastEntityPartial(
        id = podcast.id,
        title = podcast.title.trim(),
        description = podcast.description.trim(),
        image = podcast.image,
        publisher = podcast.publisher.trim(),
        explicitContent = podcast.explicitContent,
        episodeCount = podcast.episodeCount,
        latestPubDate = podcast.latestPubDate,
        earliestPubDate = podcast.earliestPubDate,
        updateDate = System.currentTimeMillis()
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
    earliestPubDate = earliestPubDate,
    updateDate = System.currentTimeMillis()
)

/** Convert a [PodcastWrapper.episodes] list to a [EpisodeEntity] list. */
fun PodcastWrapper.episodesToDatabase(): List<EpisodeEntity> = episodes.map {
    EpisodeEntity(
        id = it.id,
        title = it.title.trim(),
        description = it.description.trim(),
        podcastTitle = title.trim(),
        publisher = publisher.trim(),
        image = it.image,
        audio = it.audio,
        audioLength = it.audioLength,
        podcastId = id,
        explicitContent = it.explicitContent,
        date = it.date,
        position = 0L,
        lastPlayedDate = 0L,
        isCompleted = false,
        completionDate = 0L,
        inBookmarks = false,
        addedToBookmarksDate = 0L,
    )
}

/** Convert a [BestPodcastsWrapper] object to a [PodcastEntityPartialWithGenre] list. */
fun BestPodcastsWrapper.toDatabase(): List<PodcastEntityPartialWithGenre> {
    val currentTime = System.currentTimeMillis()
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
            earliestPubDate = it.earliestPubDate,
            genreId = this.genreId,
            updateDate = currentTime
        )
    }
}