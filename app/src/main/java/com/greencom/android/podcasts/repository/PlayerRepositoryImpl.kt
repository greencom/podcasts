package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.EpisodeEntityState
import javax.inject.Inject
import javax.inject.Singleton

private const val EPISODE_START_THRESHOLD = 60_000
private const val EPISODE_END_THRESHOLD = 90_000

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val episodeDao: EpisodeDao,
) : PlayerRepository {

    override suspend fun updateEpisodeState(episodeId: String, position: Long, duration: Long) {
        val positionEnoughForCompletion = duration - EPISODE_END_THRESHOLD
        val episodeState = when {
            position < EPISODE_START_THRESHOLD -> {
                EpisodeEntityState(episodeId, 0L, false)
            }
            position in EPISODE_START_THRESHOLD..positionEnoughForCompletion -> {
                EpisodeEntityState(episodeId, position, false)
            }
            // position > positionEnoughForCompletion.
            else -> {
                EpisodeEntityState(episodeId, 0L, true)
            }
        }
        episodeDao.update(episodeState)
    }
}