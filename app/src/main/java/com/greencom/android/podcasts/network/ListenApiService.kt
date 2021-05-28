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
     *              e.g., "game of thrones". Otherwise, it is fuzzy search.
     * @param sortByDate sort by date or not. If `0`, then sort by relevance. If `1`,
     *                   then sort by date. Default value is `0`.
     * @param type what type of contents to search for. Note: always `episode`,
     *             if needed to search for podcasts, use [searchPodcast] instead.
     * @param offset offset for pagination. Use `nextOffset` from response for this
     *               parameter. Default value is `0`.
     * @return A [SearchEpisodeWrapper] object.
     */
    @Headers("X-ListenAPI-Key: $LISTEN_API_KEY")
    @GET("search")
    suspend fun searchEpisode(
            @Query("q") query: String,
            @Query("sort_by_date") sortByDate: Int = 0,
            @Query("type") type: String = "episode",
            @Query("offset") offset: Int = 0,
    ): SearchEpisodeWrapper

    /**
     * Search for podcasts.
     *
     * @param query search query. Double quotes can be used to do verbatim match,
     *              e.g., "game of thrones". Otherwise, it is fuzzy search.
     * @param sortByDate sort by date or not. If `0`, then sort by relevance. If `1`,
     *                   then sort by date. Default value is `0`.
     * @param type what type of contents to search for. Note: always `podcast`,
     *             if needed to search for episodes, use [searchEpisode] instead.
     * @param offset offset for pagination. Use `nextOffset` from response for this
     *               parameter. Default value is `0`.
     * @return A [SearchPodcastWrapper] object.
     */
    @Headers("X-ListenAPI-Key: $LISTEN_API_KEY")
    @GET("search")
    suspend fun searchPodcast(
            @Query("q") query: String,
            @Query("sort_by_date") sortByDate: Int = 0,
            @Query("type") type: String = "podcast",
            @Query("offset") offset: Int = 0,
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
    @Headers("X-ListenAPI-Key: $LISTEN_API_KEY")
    @GET("podcasts/{id}")
    suspend fun getPodcast(
        @Path("id") id: String,
        @Query("next_episode_pub_date") nextEpisodePubDate: Long?,
        @Query("sort") sort: String = "recent_first",
    ): PodcastWrapper

    /**
     * Get a list of curated best podcasts by genre. If `genreId` is not specified,
     * returns overall best podcasts.
     *
     * @param genreId what genre podcasts to get. Use `0` to get the overall best podcasts.
     * @param page page number of the response. Default is `1`.
     * @param region filter best podcasts by country/region. Default is `ru`.
     * @return A [BestPodcastsWrapper] object.
     */
    @Headers("X-ListenAPI-Key: $LISTEN_API_KEY")
    @GET("best_podcasts")
    suspend fun getBestPodcasts(
        @Query("genre_id") genreId: Int,
        @Query("page") page: Int = 1,
        @Query("region") region: String = "ru",
    ): BestPodcastsWrapper

    /**
     * Get podcast genres that are supported in Listen Notes. The genre ID can be
     * passed to other endpoints as a parameter to get podcasts in a specific genre.
     *
     * @return A [GenresWrapper] object.
     */
    @Headers("X-ListenAPI-Key: $LISTEN_API_KEY")
    @GET("genres")
    suspend fun getGenres(): GenresWrapper
}