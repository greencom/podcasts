package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.domain.Episode
import kotlinx.coroutines.flow.Flow

/** Interface that defines player repository that contains player-related use cases. */
interface PlayerRepository {

    /** Get an episode by ID. */
    suspend fun getEpisode(episodeId: String): Episode?

    /** Get the episode's last position by ID. */
    suspend fun getEpisodePosition(episodeId: String): Long?

    /** Save player playback speed. */
    suspend fun setPlaybackSpeed(playbackSpeed: Float)

    /** Get a Flow with a player playback speed. */
    fun getPlaybackSpeed(): Flow<Float?>

    /** Save the ID of the episode that was last played. */
    suspend fun setLastPlayedEpisodeId(episodeId: String)

    /** Get a Flow with the ID of the episode that was last played. */
    fun getLastPlayedEpisodeId(): Flow<String?>

    /** Update the episode state (position, isCompleted) depending on the last position. */
    suspend fun updateEpisodeState(episodeId: String, position: Long, duration: Long)

    /** Mark an episode as completed or reset its progress by ID. */
    suspend fun onEpisodeIsCompletedChange(episodeId: String, isCompleted: Boolean)
}