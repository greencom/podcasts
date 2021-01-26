package com.greencom.android.podcasts.data

import com.squareup.moshi.Json

/** Model class for `ListenApiService.searchEpisode()` response. */
data class SearchEpisodeResult(
        /** The number of search results in this page. */
        val count: Int,
        /** The total number of search results. */
        val total: Int,
        /** A list of search results. */
        @Json(name = "results") val episodes: List<SearchEpisodeResultItem>,
        /**
         * Pass this value to the `offset` parameter of `searchEpisode()` to do
         * pagination of search results.
         */
        @Json(name = "next_offset") val nextOffset: Int,
)

/** Model class for [SearchEpisodeResult.episodes]. */
data class SearchEpisodeResultItem(
        /** Episode ID. */
        val id: String,
        /** Episode title. */
        @Json(name = "title_original") val title: String,
        /** Episode description. */
        @Json(name = "description_original") val description: String,
        /** Image URL for this episode. */
        val image: String,
        /** Whether this podcast contains explicit language. */
        @Json(name = "explicit_content") val explicitContent: Boolean,
        /** Audio URL for this episode. */
        val audio: String,
        /** Audio length of this episode in seconds. */
        @Json(name = "audio_length_sec") val audioLengthSec: Int,
        /** Published date for this episode in millisecond. */
        @Json(name = "pub_date_ms") val pubDateMs: Long,
        /** The podcast that this episode belongs to. */
        val podcast: ParentPodcast,
)

/** Model class for [SearchEpisodeResultItem.podcast]. */
data class ParentPodcast(
        /** Podcast ID. */
        val id: String,
        /** Podcast name. */
        @Json(name = "title_original") val title: String,
)
