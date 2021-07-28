package com.greencom.android.podcasts.data.datastore

import kotlinx.coroutines.flow.Flow

/** Provides access to the storage that uses DataStore APIs. */
interface PreferenceStorage {

    /** Get last played episode ID. */
    fun getLastEpisodeId(): Flow<String?>

    /** Save last played episode ID. */
    suspend fun setLastEpisodeId(episodeId: String)
}