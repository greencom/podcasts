package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.EpisodeEntityState
import javax.inject.Inject
import javax.inject.Singleton

private const val EPISODE_START_THRESHOLD = 60_000
private const val EPISODE_END_THRESHOLD = 60_000
private const val EPISODE_SKIP_BACKWARD = 5_000

// TODO
@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val episodeDao: EpisodeDao,
) : PlayerRepository {

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
                } else position
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
    }
}