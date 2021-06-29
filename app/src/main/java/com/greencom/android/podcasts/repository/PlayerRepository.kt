package com.greencom.android.podcasts.repository

/** Interface that defines player repository that contains player-related use cases. */
interface PlayerRepository {

    /** Update episode state depending on the last position. */
    suspend fun updateEpisodeState(episodeId: String, position: Long, duration: Long)
}