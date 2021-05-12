package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.flow.Flow

/**
 * App repository interface. Provides access to the ListenAPI network service
 * and the app database tables.
 */
interface Repository {

    // TODO
    fun getPodcast(id: String): Flow<State<Podcast>>

    // TODO
    fun getEpisodes(id: String): Flow<List<Episode>>

    // TODO
    suspend fun fetchRecentEpisodes(id: String)

    // TODO
    suspend fun fetchMoreEpisodes(id: String, nextEpisodePubDate: Long)

    /** Update subscription to a Podcast by ID with a given value. */
    suspend fun updateSubscription(podcastId: String, subscribed: Boolean)

    /**
     * Return the best podcasts for a given genre ID. The result presented by instances of
     * [State]. If the database already contains the appropriate podcasts, return them.
     * Otherwise, fetch the podcasts from ListenAPI and insert them into the database.
     */
    fun getBestPodcasts(genreId: Int): Flow<State<List<PodcastShort>>>

    /**
     * Fetch the best podcasts for a given genre ID from ListenAPI and insert them
     * into the database. Returns result represented by [State].
     */
    suspend fun fetchBestPodcasts(genreId: Int): State<Unit>

    /**
     * Refresh the best podcasts for a given genre ID. A new list will be fetched from
     * ListenAPI and inserted into the database. Podcasts that not anymore on the best list
     * will be excluded from it, but remain in the database. Returns result represented by
     * [State].
     */
    suspend fun refreshBestPodcasts(genreId: Int, currentList: List<PodcastShort>): State<Unit>
}