package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.flow.Flow

/**
 * App repository interface. Provides access to the ListenAPI network service
 * and the app database tables.
 */
interface Repository {

    /**
     * Get a Flow with a list of the best podcasts for a given genre ID. The result
     * is presented as [State] object.
     */
    fun getBestPodcasts(genreId: Int): Flow<State>
}