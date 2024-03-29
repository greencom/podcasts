package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.EpisodeEntityBookmark
import com.greencom.android.podcasts.data.database.EpisodeEntityState
import com.greencom.android.podcasts.data.datastore.PreferenceStorage
import com.greencom.android.podcasts.data.domain.Episode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

private const val EPISODE_START_THRESHOLD = 20_000
private const val EPISODE_END_THRESHOLD = 45_000
private const val EPISODE_SEEK_BACKWARD = 2_500

/** Implements [PlayerRepository] interface. */
@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val preferenceStorage: PreferenceStorage,
    private val episodeDao: EpisodeDao,
) : PlayerRepository {

    override suspend fun getEpisode(episodeId: String): Episode? {
        return episodeDao.getEpisode(episodeId)
    }

    override suspend fun getEpisodePosition(episodeId: String): Long? {
        return episodeDao.getEpisodePosition(episodeId)
    }

    override suspend fun setPlaybackSpeed(playbackSpeed: Float) {
        preferenceStorage.setPlaybackSpeed(playbackSpeed)
    }

    override fun getPlaybackSpeed(): Flow<Float?> {
        return preferenceStorage.getPlaybackSpeed()
    }

    override suspend fun setLastPlayedEpisodeId(episodeId: String) {
        preferenceStorage.setLastEpisodeId(episodeId)
    }

    override fun getLastPlayedEpisodeId(): Flow<String?> {
        return preferenceStorage.getLastEpisodeId()
    }

    override suspend fun updateEpisodeState(episodeId: String, position: Long, duration: Long) {
        val positionEnoughForCompletion = duration - EPISODE_END_THRESHOLD
        val episodeState = when {
            position < EPISODE_START_THRESHOLD -> return
            position in EPISODE_START_THRESHOLD..positionEnoughForCompletion -> {
                val newPosition = if (position >= EPISODE_START_THRESHOLD + EPISODE_SEEK_BACKWARD) {
                    position - EPISODE_SEEK_BACKWARD
                } else {
                    position
                }
                EpisodeEntityState(
                    id = episodeId,
                    position = newPosition,
                    lastPlayedDate = System.currentTimeMillis(),
                    isCompleted = false,
                    completionDate = 0L
                )
            }
            // position > positionEnoughForCompletion.
            else -> {
                EpisodeEntityState(
                    id = episodeId,
                    position = 0L,
                    lastPlayedDate = System.currentTimeMillis(),
                    isCompleted = true,
                    completionDate = System.currentTimeMillis()
                )
            }
        }
        episodeDao.update(episodeState)

        // Remove from the bookmarks if the episode is completed.
        // Return if the episode is not in the bookmarks or is not completed.
        if (episodeDao.isEpisodeInBookmarks(episodeId) != true || !episodeState.isCompleted) return
        episodeDao.update(
            EpisodeEntityBookmark(
                id = episodeId,
                inBookmarks = !episodeState.isCompleted,
                addedToBookmarksDate = episodeState.completionDate
            )
        )
    }

    override suspend fun onEpisodeIsCompletedChange(episodeId: String, isCompleted: Boolean) {
        val episodeState = if (isCompleted) {
            EpisodeEntityState(
                id = episodeId,
                position = 0L,
                lastPlayedDate = System.currentTimeMillis(),
                isCompleted = true,
                completionDate = System.currentTimeMillis()
            )
        } else {
            EpisodeEntityState(
                id = episodeId,
                position = 0L,
                lastPlayedDate = System.currentTimeMillis(),
                isCompleted = false,
                completionDate = 0L
            )
        }
        episodeDao.update(episodeState)

        // Remove the episode from the bookmarks if it is completed.
        if (isCompleted) {
            episodeDao.update(
                EpisodeEntityBookmark(
                    id = episodeId,
                    inBookmarks = false,
                    addedToBookmarksDate = episodeState.completionDate
                )
            )
        }
    }
}