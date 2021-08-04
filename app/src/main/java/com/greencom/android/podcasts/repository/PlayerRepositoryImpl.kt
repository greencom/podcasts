package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.EpisodeEntityPlaylist
import com.greencom.android.podcasts.data.database.EpisodeEntityState
import com.greencom.android.podcasts.data.datastore.PreferenceStorage
import com.greencom.android.podcasts.data.domain.Episode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val EPISODE_START_THRESHOLD = 30_000
private const val EPISODE_END_THRESHOLD = 60_000
private const val EPISODE_SKIP_BACKWARD = 5_000

/** Implements [PlayerRepository] interface. */
@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val preferenceStorage: PreferenceStorage,
    private val episodeDao: EpisodeDao,
) : PlayerRepository {

    override fun getLastEpisodeId(): Flow<String?> {
        return preferenceStorage.getLastEpisodeId()
    }

    override suspend fun setLastEpisodeId(episodeId: String) {
        preferenceStorage.setLastEpisodeId(episodeId)
    }

    override suspend fun getEpisode(episodeId: String): Episode? {
        return episodeDao.getEpisode(episodeId)
    }

    override suspend fun getEpisodePosition(episodeId: String): Long? {
        return episodeDao.getEpisodePosition(episodeId)
    }

    override suspend fun updateEpisodeState(episodeId: String, position: Long, duration: Long) {
        val positionEnoughForCompletion = duration - EPISODE_END_THRESHOLD
        val episodeState = when {
            position < EPISODE_START_THRESHOLD -> {
                EpisodeEntityState(
                    id = episodeId,
                    position = 0L,
                    isCompleted = false,
                    completionDate = 0L
                )
            }
            position in EPISODE_START_THRESHOLD..positionEnoughForCompletion -> {
                val newPosition = if (position >= EPISODE_START_THRESHOLD + EPISODE_SKIP_BACKWARD) {
                    position - EPISODE_SKIP_BACKWARD
                } else {
                    position
                }
                EpisodeEntityState(
                    id = episodeId,
                    position = newPosition,
                    isCompleted = false,
                    completionDate = 0L
                )
            }
            // position > positionEnoughForCompletion.
            else -> {
                EpisodeEntityState(
                    id = episodeId,
                    position = 0L,
                    isCompleted = true,
                    completionDate = System.currentTimeMillis()
                )
            }
        }
        episodeDao.update(episodeState)

        // Remove from the playlist if the episode is completed.
        // Return if the episode is not in the playlist.
        if (episodeDao.isEpisodeInPlaylist(episodeId) != true) return
        episodeDao.update(EpisodeEntityPlaylist(
            id = episodeId,
            inPlaylist = !episodeState.isCompleted,
            addedToPlaylistDate = episodeState.completionDate
        ))
    }

    override suspend fun markEpisodeCompleted(episodeId: String) {
        val episodeState = EpisodeEntityState(
            id = episodeId,
            position = 0L,
            isCompleted = true,
            completionDate = System.currentTimeMillis()
        )
        episodeDao.update(episodeState)

        // Remove the episode from the playlist.
        episodeDao.update(EpisodeEntityPlaylist(
            id = episodeId,
            inPlaylist = false,
            addedToPlaylistDate = episodeState.completionDate
        ))
    }
}