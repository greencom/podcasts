package com.greencom.android.podcasts.network

import com.greencom.android.podcasts.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/** ListenAPI base URL. */
private const val BASE_URL = "https://listen-api.listennotes.com/api/v2/"

/** Log setup. */
private val logging = HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) }
private val httpClient = OkHttpClient.Builder().apply { addInterceptor(logging) }

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .client(httpClient.build()) // Log.
    .build()

interface ListenApiService {

    /**
     * Search for episodes.
     *
     * @param query search query. Double quotes can be used to do verbatim match,
     *              e.g., `"game of thrones"`. Otherwise, it is fuzzy search.
     * @param sortByDate sort by date or not. If `0`, then sort by relevance. If `1`,
     *                   then sort by date. Default value is `0`.
     * @param type what type of contents to search for. Note: always `episode`,
     *             if needed to search for podcasts, use [ListenApiService.searchPodcast]
     *             instead.
     * @param offset offset for pagination. Use `nextOffset` from response for this parameter.
     *               Default value is `0`.
     * @param onlyIn a comma-delimited string to search only in specific fields.
     *               Allowed values are title, description, author, and audio.
     *               Default value is `title,description,author`.
     * @param safeMode whether or not to exclude podcasts with explicit language.
     *                 1 is yes, and 0 is no. Default value is `0`.
     *
     * @return A [SearchEpisodeResult] object.
     */
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("search")
    suspend fun searchEpisode(
            @Query("q") query: String,
            @Query("sort_by_date") sortByDate: Int = 0,
            @Query("type") type: String = "episode",
            @Query("offset") offset: Int = 0,
            @Query("only_in") onlyIn: String = "title,description,author",
            @Query("safe_mode") safeMode: Int = 0,
    ): SearchEpisodeResult

    /**
     * Search for podcasts.
     *
     * @param query search query. Double quotes can be used to do verbatim match,
     *              e.g., `"game of thrones"`. Otherwise, it is fuzzy search.
     * @param sortByDate sort by date or not. If `0`, then sort by relevance. If `1`,
     *                   then sort by date. Default value is `0`.
     * @param type what type of contents to search for. Note: always `podcast`,
     *             if needed to search for episodes, use [ListenApiService.searchEpisode]
     *             instead.
     * @param offset offset for pagination. Use `nextOffset` from response for this parameter.
     *               Default value is `0`.
     * @param onlyIn a comma-delimited string to search only in specific fields.
     *               Allowed values are title, description, author, and audio.
     *               Default value is `title,description,author`.
     * @param safeMode whether or not to exclude podcasts with explicit language.
     *                 1 is yes, and 0 is no. Default value is `0`.
     *
     * @return A [SearchPodcastResult] object.
     */
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("search")
    suspend fun searchPodcast(
            @Query("q") query: String,
            @Query("sort_by_date") sortByDate: Int = 0,
            @Query("type") type: String = "podcast",
            @Query("offset") offset: Int = 0,
            @Query("only_in") onlyIn: String = "title,description,author",
            @Query("safe_mode") safeMode: Int = 0,
    ): SearchPodcastResult

    /**
     * Get podcast [Genres] that are supported in Listen Notes. The [Genre] id can be
     * passed to other endpoints as a parameter to get podcasts in a specific [Genre].
     *
     * @return A [Genres] object.
     */
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("genres")
    suspend fun getGenres(): Genres

    /**
     * Get a list of curated [BestPodcasts] by genre. If `genreId` is not specified,
     * returns overall [BestPodcasts].
     *
     * @param genreId what genre podcasts to get. Default value is `0`.
     * @param page page number of response. Default value is `1`.
     * @param region filter best podcasts by country/region. Default value is `ru`.
     * @param safeMode whether or not to exclude podcasts with explicit language.
     *                 1 is yes, and 0 is no. Default value is `0`.
     *
     * @return A [BestPodcasts] object.
     */
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("best_podcasts")
    suspend fun getBestPodcasts(
            @Query("genre_id") genreId: Int = 0,
            @Query("page") page: Int = 1,
            @Query("region") region: String = "ru",
            @Query("safe_mode") safeMode: Int = 0,
    ): BestPodcasts
}

/** ListenAPI service object. */
object ListenApi {
    val retrofitService: ListenApiService by lazy { retrofit.create(ListenApiService::class.java) }
}
