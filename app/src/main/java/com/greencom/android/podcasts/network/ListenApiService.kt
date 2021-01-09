package com.greencom.android.podcasts.network

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

private const val BASE_URL = "https://listen-api.listennotes.com/api/v2/"

// Logging
private val logging = HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) }
private val httpClient = OkHttpClient.Builder().apply { addInterceptor(logging) }

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .client(httpClient.build()) // Logging
    .build()

interface ListenApiService {

    // TODO: Improve
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("best_podcasts")
    suspend fun getBestPodcasts(
        @Query("page") page: Int = 1,
        @Query("region") region: String = "ru",
        @Query("safe_mode") safeMode: Int = 0,
    ): BestPodcasts

    // TODO: Improve
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("best_podcasts")
    suspend fun getBestPodcastsByGenre(
        @Query("genre_id") genreId: Int = 111,
        @Query("page") page: Int = 1,
        @Query("region") region: String = "ru",
        @Query("safe_mode") safeMode: Int = 0,
    ): BestPodcasts
}

object ListenApi {
    val retrofitService: ListenApiService by lazy { retrofit.create(ListenApiService::class.java) }
}

// TODO: Temp
data class BestPodcasts(
//        @Json(name = "id") val genreId: Int,
//        @Json(name = "name") val genreName: String,
//        @Json(name = "total") val podcastCount: Int,
//        @Json(name = "has_next") val hasNextPage: Boolean,
        val podcasts: List<Podcast>,
//        @Json(name = "parent_id") val parentGenreId: Int?,
//        @Json(name = "page_number") val pageNumber: Int,
//        @Json(name = "has_previous") val hasPreviousPage: Boolean,
//        @Json(name = "listennotes_url") val listennotesUrl: String,
//        @Json(name = "next_page_number") val nextPageNumber: Int,
//        @Json(name = "previous_page_number") val previousPageNumber: Int,
)

data class Podcast(
        val id: String,
//        val rss: String,
//        val type: String,
//        val email: String,
//        val extra: Extra,
        val image: String,
        val title: String,
//        val country: String,
//        val website: String?,
        val language: String,
        @Json(name = "genre_ids") val genreIds: List<Int>,
//        @Json(name = "itunes_id") val itunesId: Long,
        val publisher: String,
//        val thumbnail: String,
//        @Json(name = "is_claimed") val isClaimed: Boolean,
        val description: String,
//        @Json(name = "looking_for") val lookingFor: LookingFor,
//        @Json(name = "listen_score") val listenScore: Int?,
        @Json(name = "total_episodes") val episodesCount: Int,
//        @Json(name = "listennotes_url") val listennotesUrl: String,
        @Json(name = "explicit_content") val explicitContent: Boolean,
//        @Json(name = "latest_pub_date_ms") val latestPubDateMs: Long,
//        @Json(name = "earliest_pub_date_ms") val earliestPubDateMs: Long,
//        @Json(name = "listen_score_global_rank") val listenScoreGlobalRank: String?,
)

data class Extra(
        val url1: String,
        val url2: String,
        val url3: String,
        @Json(name = "google_url") val googleUrl: String,
        @Json(name = "spotify_url") val spotifyUrl: String,
        @Json(name = "youtube_url") val youtubeUrl: String,
        @Json(name = "linkedin_url") val linkedinUrl: String,
//        @Json(name = "wechat_handle") val wechatHandle: String,
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