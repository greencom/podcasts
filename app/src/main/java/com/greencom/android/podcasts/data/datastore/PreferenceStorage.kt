package com.greencom.android.podcasts.data.datastore

import kotlinx.coroutines.flow.Flow

/** Provides access to the storage that uses DataStore APIs. */
interface PreferenceStorage {

    /** Save player playback speed. */
    suspend fun setPlaybackSpeed(playbackSpeed: Float)

    /** Get a Flow with a player playback speed. */
    fun getPlaybackSpeed(): Flow<Float?>

    /** Save last played episode ID. */
    suspend fun setLastEpisodeId(episodeId: String)

    /** Get last played episode ID. */
    fun getLastEpisodeId(): Flow<String?>
}