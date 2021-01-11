package com.greencom.android.podcasts.data

import com.squareup.moshi.Json

data class SearchEpisodeResult(
        val count: Int,
        val total: Int,
        val results: List<SearchEpisodeResultItems>,
        @Json(name = "next_offset") val nextOffset: Int,
)

data class SearchEpisodeResultItems(
        val id: String,
        val link: String,
        val audio: String,
        val image: String,
        val podcast: BelongsTo,
        @Json(name = "pub_date_ms") val pubDateMs: Long,
        @Json(name = "title_original") val title: String,
        @Json(name = "audio_length_sec") val audioLengthSec: Int,
        @Json(name = "explicit_content") val explicitContent: Boolean,
        @Json(name = "description_original") val description: String,
)

data class BelongsTo(
        val id: String,
        val image: String,
        @Json(name = "genre_ids") val genreIds: List<Int>,
        @Json(name = "title_original") val title: String,
        @Json(name = "publisher_original") val publisher: String,
)
