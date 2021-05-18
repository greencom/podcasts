package com.greencom.android.podcasts.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.greencom.android.podcasts.data.domain.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Interface to interact with the `episodes` table. */
@Dao
abstract class EpisodeDao {

    /**
     * Insert an [EpisodeEntity] object into the database. [OnConflictStrategy.IGNORE]
     * on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(episode: EpisodeEntity)

    /**
     * Insert an [EpisodeEntity] list into the database. [OnConflictStrategy.IGNORE]
     * on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(episodes: List<EpisodeEntity>)

    /**
     * Get the publication date of the latest podcast episode which was uploaded
     * to the database for a given podcast ID.
     */
    @Query("SELECT date from episodes WHERE podcast_id = :podcastId ORDER BY date DESC LIMIT 1")
    abstract suspend fun getLatestLoadedEpisodePubDate(podcastId: String): Long?

    /**
     * Get the publication date of the earliest podcast episode which was uploaded
     * to the database for a given podcast ID.
     */
    @Query("SELECT date from episodes WHERE podcast_id = :podcastId ORDER BY date ASC LIMIT 1")
    abstract suspend fun getEarliestLoadedEpisodePubDate(podcastId: String): Long?

    /** Get the number of loaded episodes for a given podcast ID. */
    @Query("SELECT COUNT(id) FROM episodes WHERE podcast_id = :podcastId")
    abstract suspend fun getEpisodeCount(podcastId: String): Int

    /**
     * Get a Flow with a list of episodes for a given podcast ID. Episodes are sorted
     * by date in descending order. No need to apply [distinctUntilChanged] function
     * since it is already done under the hood.
     */
    fun getEpisodesFlow(podcastId: String) = getEpisodesFlowRaw(podcastId).distinctUntilChanged()



    // Helper methods start.

    /**
     * Get a Flow with a list of episodes for a given podcast ID. Episodes are sorted
     * by date in descending order.
     */
    @Query(
        "SELECT id, title, description, image, audio, audio_length, podcast_id, " +
                "explicit_content, date " +
                "FROM episodes " +
                "WHERE podcast_id = :podcastId " +
                "ORDER BY date DESC")
    protected abstract fun getEpisodesFlowRaw(podcastId: String): Flow<List<Episode>>

    // Helper methods end.
}