package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.flow.Flow

/**
 * App repository interface. Provides access to the ListenAPI network service
 * and the app database tables.
 */
interface Repository {

    // TODO
    fun getBestPodcasts(genreId: Int): Flow<State>
}