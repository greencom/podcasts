package com.greencom.android.podcasts.data

import com.squareup.moshi.Json

/** Model class for `ListenApiService.searchPodcast()` response. */
data class SearchPodcastResult(
        /** The number of search results in this page. */
        val count: Int,
        /** The total number of search results. */
        val total: Int,
        /** A list of search results. */
        @Json(name = "results") val podcasts: List<SearchPodcastResultItem>,
        /**
         * Pass this value to the `offset` parameter of `searchPodcast()` to do
         * pagination of search results.
         */
        @Json(name = "next_offset") val nextOffset: Int,
)

/** Model class for [SearchPodcastResult.podcasts]. */
data class SearchPodcastResultItem(
        /** Podcast ID. */
        val id: String,
        /** Podcast name. */
        @Json(name = "title_original") val title: String,
        /** Podcast description. */
        @Json(name = "description_original") val description: String,
        /** Image URL for this podcast. */
        val image: String,
        /** Whether this podcast contains explicit language. */
        @Json(name = "explicit_content") val explicitContent: Boolean,
        /** Podcast publisher. */
        @Json(name = "publisher_original") val publisher: String,
)

/** Model class for `ListenApiService.getBestPodcasts()` response. */
data class BestPodcasts(
        /** A list of search results. */
        val podcasts: List<BestPodcastsResultItem>,
        /** Genre ID. */
        @Json(name = "id") val genreId: Int,
        /** Genre name. */
        @Json(name = "name") val genreName: String,
        /** Whether there is the next page of response. */
        @Json(name = "has_next") val hasNextPage: Boolean,
)

/** Model class for [BestPodcasts.podcasts]. */
data class BestPodcastsResultItem(
        /** Podcast ID. */
        val id: String,
        /** Podcast name. */
        val title: String,
        /** Image URL for this podcast. */
        val image: String,
        /** Podcast publisher. */
        val publisher: String,
        /** Whether this podcast contains explicit language. */
        @Json(name = "explicit_content") val explicitContent: Boolean,
)
