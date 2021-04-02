package com.greencom.android.podcasts.network

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface that defines the methods for interacting with ListenAPI.
 * Available methods: [searchEpisode], [searchPodcast], [getPodcast], [getBestPodcasts],
 * [getGenres].
 */
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
     * @param offset offset for pagination. Use `nextOffset` from response for this
     *               parameter. Default value is `0`.
     * @param onlyIn a comma-delimited string to search only in specific fields.
     *               Allowed values are title, description, author, and audio.
     *               Default value is `title,description,author`.
     * @param safeMode whether or not to exclude podcasts with explicit language.
     *                 `1` is yes, and `0` is no. Default value is `0`.
     * @return A [SearchEpisodeWrapper] object.
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
    ): SearchEpisodeWrapper

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
     * @param offset offset for pagination. Use `nextOffset` from response for this
     *               parameter. Default value is `0`.
     * @param onlyIn a comma-delimited string to search only in specific fields.
     *               Allowed values are title, description, author, and audio.
     *               Default value is `title,description,author`.
     * @param safeMode whether or not to exclude podcasts with explicit language.
     *                 `1` is yes, and `0` is no. Default value is `0`.
     * @return A [SearchPodcastWrapper] object.
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
    ): SearchPodcastWrapper

    /**
     * Fetch detailed meta data and episodes for a specific podcast (up to 10 episodes
     * each time). Use the [nextEpisodePubDate] parameter to do pagination and fetch more
     * episodes.
     *
     * @param id Podcast ID.
     * @param nextEpisodePubDate For episode pagination. It is value of
     *                           `next_episode_pub_date` from the response of last request.
     *                           If not specified, just return latest 10 episodes or oldest
     *                           10 episodes, depending on the value of the [sort] parameter.
     * @param sort Sort the episodes of this podcast. Available values: `recent_first`,
     *             `oldest_first`. Default is `recent_first`.
     * @return A [PodcastWrapper] object.
     */
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("podcasts/{id}")
    suspend fun getPodcast(
        @Path("id") id: String,
        @Query("next_episode_pub_date") nextEpisodePubDate: Long,
        @Query("sort") sort: String,
    ): PodcastWrapper

    /**
     * Get a list of curated best podcasts by genre. If `genreId` is not specified,
     * returns overall best podcasts.
     *
     * @param genreId what genre podcasts to get. Default value is `0`.
     * @param page page number of response. Default value is `1`.
     * @param region filter best podcasts by country/region. Default value is `ru`.
     * @param safeMode whether or not to exclude podcasts with explicit language.
     *                 `1` is yes, and `0` is no. Default value is `0`.
     * @return A [BestPodcastsWrapper] object.
     */
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("best_podcasts")
    suspend fun getBestPodcasts(
        @Query("genre_id") genreId: Int = 0,
        @Query("page") page: Int = 1,
        @Query("region") region: String = "ru",
        @Query("safe_mode") safeMode: Int = 0,
    ): BestPodcastsWrapper

    /**
     * Get podcast genres that are supported in Listen Notes. The genre id can be
     * passed to other endpoints as a parameter to get podcasts in a specific genre.
     *
     * @param topLevelOnly whether or not to get only top level genres. `1` is get
     *                     only top level genres, `0` is get all genres. Default
     *                     value is `0`.
     * @return A [GenresWrapper] object.
     */
    @Headers("X-ListenAPI-Key: $LISTENAPI_KEY")
    @GET("genres")
    suspend fun getGenres(
            @Query("top_level_only") topLevelOnly: Int = 0
    ): GenresWrapper
}