package com.greencom.android.podcasts.data

import com.squareup.moshi.Json

data class Podcast(
        val id: String,
        val title: String,
        val description: String,
        val image: String,
        val publisher: String,
        val language: String,
        @Json(name = "genre_ids") val genreIds: List<Int>,
        @Json(name = "total_episodes") val episodesCount: Int,
        @Json(name = "explicit_content") val explicitContent: Boolean,
//        @Json(name = "itunes_id") val itunesId: Long,
//        @Json(name = "listen_score") val listenScore: Int?,
//        val rss: String,
//        val type: String,
//        val email: String,
//        val extra: Extra,
//        val country: String,
//        val website: String?,
//        val thumbnail: String,
//        @Json(name = "is_claimed") val isClaimed: Boolean,
//        @Json(name = "looking_for") val lookingFor: LookingFor,
//        @Json(name = "listennotes_url") val listennotesUrl: String,
//        @Json(name = "latest_pub_date_ms") val latestPubDateMs: Long,
//        @Json(name = "earliest_pub_date_ms") val earliestPubDateMs: Long,
//        @Json(name = "listen_score_global_rank") val listenScoreGlobalRank: String?,
)

data class SearchPodcastResult(
        val count: Int,
        val total: Int,
        val results: List<SearchPodcastResultItems>,
        @Json(name = "next_offset") val nextOffset: Int,
)

data class SearchPodcastResultItems(
        val id: String,
        @Json(name = "title_original") val title: String,
        @Json(name = "description_original") val description: String,
        val image: String,
        @Json(name = "publisher_original") val publisher: String,
        @Json(name = "explicit_content") val explicitContent: Boolean,
)

data class BestPodcasts(
        val podcasts: List<Podcast>,
        @Json(name = "id") val genreId: Int,
        @Json(name = "name") val genreName: String,
        @Json(name = "has_next") val hasNextPage: Boolean,
//        @Json(name = "total") val podcastCount: Int,
//        @Json(name = "parent_id") val parentGenreId: Int?,
//        @Json(name = "page_number") val pageNumber: Int,
//        @Json(name = "has_previous") val hasPreviousPage: Boolean,
//        @Json(name = "listennotes_url") val listennotesUrl: String,
//        @Json(name = "next_page_number") val nextPageNumber: Int,
//        @Json(name = "previous_page_number") val previousPageNumber: Int,
)

data class Extra(
        val url1: String,
        val url2: String,
        val url3: String,
        @Json(name = "google_url") val googleUrl: String,
        @Json(name = "spotify_url") val spotifyUrl: String,
        @Json(name = "youtube_url") val youtubeUrl: String,
        @Json(name = "linkedin_url") val linkedinUrl: String,
        @Json(name = "wechat_handle") val wechatHandle: String,
        @Json(name = "patreon_handle") val patreonHandle: String,
        @Json(name = "twitter_handle") val twitterHandle: String,
        @Json(name = "facebook_handle") val facebookHandle: String,
        @Json(name = "instagram_handle") val instagramHandle: String,
)

data class LookingFor(
        val guests: Boolean,
        val cohosts: Boolean,
        val sponsors: Boolean,
        @Json(name = "cross_promotion") val crossPromotion: Boolean,
)
