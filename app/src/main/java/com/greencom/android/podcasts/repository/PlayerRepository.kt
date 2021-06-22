package com.greencom.android.podcasts.repository

// TODO
interface PlayerRepository {

    suspend fun updateEpisodeState(episodeId: String, position: Long, duration: Long)
}