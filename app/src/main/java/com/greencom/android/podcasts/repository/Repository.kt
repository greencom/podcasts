package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

/**
 * App repository interface. Provides access to the ListenAPI network service
 * and the app database tables.
 */
interface Repository {

    /**
     * Return the best podcasts for a given genre ID. The result presented by instances of
     * [State]. If the database already contains the appropriate podcasts, return them.
     * Otherwise, fetch the podcasts from ListenAPI and insert them into the database.
     */
    fun getBestPodcasts(genreId: Int, dispatcher: CoroutineDispatcher): Flow<State<List<PodcastShort>>>

    /**
     * Fetch the best podcasts for a given genre ID from ListenAPI and insert them
     * into the database.
     */
    suspend fun fetchBestPodcasts(genreId: Int, dispatcher: CoroutineDispatcher): State<Unit>

    /**
     * Refresh the best podcasts for a given genre ID. A new list will be fetched from
     * ListenAPI and inserted into the database. Podcasts that not anymore on the best list
     * will be excluded from it, but remain in the database.
     */
    suspend fun refreshBestPodcasts(genreId: Int, dispatcher: CoroutineDispatcher): State<Unit>
}