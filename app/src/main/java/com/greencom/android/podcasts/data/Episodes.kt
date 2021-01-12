package com.greencom.android.podcasts.data

import com.squareup.moshi.Json

// Search for episode
data class SearchEpisodeResult(
        val count: Int,
        val total: Int,
        @Json(name = "results") val episodes: List<SearchEpisodeResultItem>,
        @Json(name = "next_offset") val nextOffset: Int,
)

data class SearchEpisodeResultItem(
        val id: String,
        @Json(name = "title_original") val title: String,
        @Json(name = "description_original") val description: String,
        val image: String,
        @Json(name = "explicit_content") val explicitContent: Boolean,
        val audio: String,
        @Json(name = "audio_length_sec") val audioLengthSec: Int,
        @Json(name = "pub_date_ms") val pubDateMs: Long,
        val podcast: ParentPodcast,
)

data class ParentPodcast(
        val id: String,
        @Json(name = "title_original") val title: String,
)
