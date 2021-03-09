package com.greencom.android.podcasts.data

import com.greencom.android.podcasts.data.database.GenreEntity
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.database.PodcastEntity
import com.greencom.android.podcasts.data.database.PodcastEntityUpdateSubscription
import com.greencom.android.podcasts.data.domain.Genre
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.network.BestPodcastsResponse
import com.greencom.android.podcasts.network.GenresResponse

/**
 * Convert a [Podcast] object to a [PodcastEntityUpdateSubscription] object
 * with reversing `inSubscription` value. [PodcastEntityUpdateSubscription] used
 * as parameter for [PodcastDao.update] to update the `inSubscription` property
 * of the existing entry.
 */
fun Podcast.updateSubscription(): PodcastEntityUpdateSubscription {
    return PodcastEntityUpdateSubscription(
        id = this.id,
        inSubscriptions = !this.inSubscriptions
    )
}

/** Convert a [PodcastEntity] object to a [Podcast]. */
fun PodcastEntity.asPodcast(): Podcast {
    return Podcast(
        id = this.id,
        title = this.title,
        description = this.description,
        image = this.image,
        publisher = this.publisher,
        explicitContent = this.explicitContent,
        episodeCount = this.episodeCount,
        latestPubDate = this.latestPubDate,
        inBestForGenre = this.inBestForGenre,
        inSubscriptions = this.inSubscriptions,
    )
}

/** Convert a [PodcastEntity] list to a list of [Podcast]s. */
fun List<PodcastEntity>.asPodcasts(): List<Podcast> {
    return map {
        Podcast(
            id = it.id,
            title = it.title,
            description = it.description,
            image = it.image,
            publisher = it.publisher,
            explicitContent = it.explicitContent,
            episodeCount = it.episodeCount,
            latestPubDate = it.latestPubDate,
            inBestForGenre = it.inBestForGenre,
            inSubscriptions = it.inSubscriptions,
        )
    }
}

/**
 * Convert a [PodcastEntity] object to a [PodcastEntityUpdateSubscription] object
 * with reversing `inSubscription` value. [PodcastEntityUpdateSubscription] used
 * as parameter for [PodcastDao.update] to update the `inSubscription` property
 * of the existing entry.
 */
fun PodcastEntity.updateSubscription(): PodcastEntityUpdateSubscription {
    return PodcastEntityUpdateSubscription(
        id = this.id,
        inSubscriptions = !this.inSubscriptions
    )
}

/** Convert [BestPodcastsResponse] object to a [PodcastEntity] list. */
fun BestPodcastsResponse.asPodcastEntities(): List<PodcastEntity> {
    return podcasts.map {
        PodcastEntity(
            id = it.id,
            title = it.title,
            description = it.description,
            image = it.image,
            publisher = it.publisher,
            explicitContent = it.explicitContent,
            episodeCount = it.episodeCount,
            latestPubDate = it.latestPubDate,
            inBestForGenre = this.genreId,
            inSubscriptions = false,
        )
    }
}

/** Convert [BestPodcastsResponse] object to a [Podcast] list. */
fun BestPodcastsResponse.asPodcasts(): List<Podcast> {
    return podcasts.map {
        Podcast(
            id = it.id,
            title = it.title,
            description = it.description,
            image = it.image,
            publisher = it.publisher,
            explicitContent = it.explicitContent,
            episodeCount = it.episodeCount,
            latestPubDate = it.latestPubDate,
            inBestForGenre = this.genreId,
            inSubscriptions = false,
        )
    }
}

/** Convert a [GenreEntity] object to a [Genre]. */
fun GenreEntity.asGenre(): Genre {
    return Genre(
        id = this.id,
        name = this.name,
        parentId = this.parentId
    )
}

/** Convert a [GenreEntity] list to a list of [Genre]s. */
fun List<GenreEntity>.asGenres(): List<Genre> {
    return map {
        Genre(
            id = it.id,
            name = it.name,
            parentId = it.parentId,
        )
    }
}

/**
 * Convert [GenresResponse] object to a [GenreEntity] list.
 *
 * If [GenresResponse.GenresResponseItem.parentId] is `null`, assign [Genre.NO_PARENT_GENRE]
 * value to the [GenreEntity.parentId] property.
 */
fun GenresResponse.asGenreEntities(): List<GenreEntity> {
    return genres.map {
        GenreEntity(
            id = it.id,
            name = it.name,
            parentId = it.parentId ?: Genre.NO_PARENT_GENRE,
        )
    }
}

/**
 * Convert [GenresResponse] object to a [Genre] list.
 *
 * If [GenresResponse.GenresResponseItem.parentId] is `null`, assign [Genre.NO_PARENT_GENRE]
 * value to the [Genre.parentId] property.
 */
fun GenresResponse.asGenres(): List<Genre> {
    return genres.map {
        Genre(
            id = it.id,
            name = it.name,
            parentId = it.parentId ?: Genre.NO_PARENT_GENRE,
        )
    }
}