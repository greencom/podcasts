package com.greencom.android.podcasts.network

import com.greencom.android.podcasts.data.database.GenreEntity
import com.greencom.android.podcasts.data.domain.Genre
import com.squareup.moshi.Json

/** Model class for `ListenApiService.searchEpisode` response. */
data class SearchEpisodeResponse(

    /** The number of search results in this page. */
    val count: Int,

    /** The total number of search results. */
    val total: Int,

    /** A list of search results. */
    @Json(name = "results")
    val episodes: List<SingleEpisodeResponse>,

    /**
     * Pass this value to the `offset` parameter of `searchEpisode()` to do
     * pagination of search results.
     */
    @Json(name = "next_offset")
    val nextOffset: Int,
)

/** Model class for single episode object in the [SearchEpisodeResponse.episodes] list. */
data class SingleEpisodeResponse(

    /** Episode ID. */
    val id: String,

    /** Episode title. */
    @Json(name = "title_original")
    val title: String,

    /** Episode description. */
    @Json(name = "description_original")
    val description: String,

    /** Image URL for this episode. */
    val image: String,

    /** Whether this podcast contains explicit language. */
    @Json(name = "explicit_content")
    val explicitContent: Boolean,

    /** Audio URL for this episode. */
    val audio: String,

    /** Audio length of this episode in seconds. */
    @Json(name = "audio_length_sec")
    val audioLengthSec: Int,

    /** Published date for this episode in millisecond. */
    @Json(name = "pub_date_ms")
    val pubDateMs: Long,

    /** The podcast that this episode belongs to. */
    val podcast: ParentPodcastResponse,
)

/** Model class for [SingleEpisodeResponse.podcast] object. */
data class ParentPodcastResponse(

    /** Podcast ID. */
    val id: String,

    /** Podcast name. */
    @Json(name = "title_original")
    val title: String,
)





/** Model class for `ListenApiService.searchPodcast` response. */
data class SearchPodcastResponse(

    /** The number of search results in this page. */
    val count: Int,

    /** The total number of search results. */
    val total: Int,

    /** A list of search results. */
    @Json(name = "results")
    val podcasts: List<SinglePodcastResponse>,

    /**
     * Pass this value to the `offset` parameter of `searchPodcast()` to do
     * pagination of search results.
     */
    @Json(name = "next_offset")
    val nextOffset: Int,
)

/** Model class for single podcast object in the [SearchPodcastResponse.podcasts] list. */
data class SinglePodcastResponse(

    /** Podcast ID. */
    val id: String,

    /** Podcast name. */
    @Json(name = "title_original")
    val title: String,

    /** Podcast description. */
    @Json(name = "description_original")
    val description: String,

    /** Image URL for this podcast. */
    val image: String,

    /** Whether this podcast contains explicit language. */
    @Json(name = "explicit_content")
    val explicitContent: Boolean,

    /** Podcast publisher. */
    @Json(name = "publisher_original")
    val publisher: String,
)





/** Model class for `ListenApiService.getBestPodcasts()` response. */
data class BestPodcastsResponse(

    /** A list of search results. */
    val podcasts: List<SingleBestPodcastResponse>,

    /** Genre ID. */
    @Json(name = "id")
    val genreId: Int,

    /** Genre name. */
    @Json(name = "name")
    val genreName: String,

    /** Whether there is the next page of response. */
    @Json(name = "has_next")
    val hasNextPage: Boolean,
)

/** Model class for single podcast object in the [BestPodcastsResponse.podcasts] list. */
data class SingleBestPodcastResponse(

    /** Podcast ID. */
    val id: String,

    /** Podcast name. */
    val title: String,

    /** Image URL for this podcast. */
    val image: String,

    /** Podcast publisher. */
    val publisher: String,

    /** Whether this podcast contains explicit language. */
    @Json(name = "explicit_content")
    val explicitContent: Boolean,
)





/** Model class for `ListenApiService.getGenres()` response. */
data class GenresResponse(val genres: List<SingleGenreResponse>)

/** Model class for single genre object in the [GenresResponse.genres] list. */
data class SingleGenreResponse(

    /** Genre ID. */
    val id: Int,

    /** Genre name. */
    val name: String,

    /** Parent genre ID. */
    @Json(name = "parent_id")
    val parentId: Int?,
)

/**
 * Convert [GenresResponse] object to a [GenreEntity] list.
 *
 * If [SingleGenreResponse.parentId] is `null`, assign [Genre.NO_PARENT_GENRE] value
 * to [GenreEntity.parentId] property.
 */
fun GenresResponse.asDatabaseModel(): List<GenreEntity> {
    return genres.map {
        GenreEntity(
            id = it.id,
            name = it.name,
            parentId = it.parentId ?: Genre.NO_PARENT_GENRE,
        )
    }
}
