package com.greencom.android.podcasts.data.datastore

import kotlinx.coroutines.flow.Flow

// TODO
interface PreferenceStorage {

    fun getLastEpisodeId(): Flow<String?>

    suspend fun setLastEpisodeId(episodeId: String)
}