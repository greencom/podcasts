package com.greencom.android.podcasts.data.datastore

import kotlinx.coroutines.flow.Flow

/** Provides access to the storage that uses DataStore APIs. */
interface PreferenceStorage {

    /** Save app theme mode. */
    suspend fun setTheme(mode: Int)

    /** Get a Flow with the app theme mode. */
    fun getTheme(): Flow<Int?>

    /** Save player playback speed. */
    suspend fun setPlaybackSpeed(playbackSpeed: Float)

    /** Get a Flow with the player playback speed. */
    fun getPlaybackSpeed(): Flow<Float?>

    /** Save the ID of the last played episode. */
    suspend fun setLastEpisodeId(episodeId: String)

    /** Get a Flow with the ID of the last played episode. */
    fun getLastEpisodeId(): Flow<String?>

    /** Save the subscription presentation mode. */
    suspend fun setSubscriptionMode(mode: Int)

    /** Get a Flow with a subscription presentation mode. */
    fun getSubscriptionMode(): Flow<Int?>
}