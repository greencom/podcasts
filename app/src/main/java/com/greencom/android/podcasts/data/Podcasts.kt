package com.greencom.android.podcasts.data

import com.squareup.moshi.Json

// Search for podcast
data class SearchPodcastResult(
        val count: Int,
        val total: Int,
        @Json(name = "results") val podcasts: List<SearchPodcastResultItem>,
        @Json(name = "next_offset") val nextOffset: Int,
)

data class SearchPodcastResultItem(
        val id: String,
        @Json(name = "title_original") val title: String,
        @Json(name = "description_original") val description: String,
        val image: String,
        @Json(name = "explicit_content") val explicitContent: Boolean,
        @Json(name = "publisher_original") val publisher: String,
)

// Best podcasts
data class BestPodcasts(
        val podcasts: List<BestPodcastsItem>,
        @Json(name = "id") val genreId: Int,
        @Json(name = "name") val genreName: String,
        @Json(name = "has_next") val hasNextPage: Boolean,
)

data class BestPodcastsItem(
        val id: String,
        val title: String,
        val image: String,
        val publisher: String,
        @Json(name = "explicit_content") val explicitContent: Boolean,
)
