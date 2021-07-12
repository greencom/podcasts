package com.greencom.android.podcasts.data.datastore

import kotlinx.coroutines.flow.Flow

/** Provides access to the storage that uses DataStore APIs. */
interface PreferenceStorage {

    fun getLastEpisodeId(): Flow<String?>

    suspend fun setLastEpisodeId(episodeId: String)
}