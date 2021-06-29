package com.greencom.android.podcasts.data.database

import androidx.room.*

/** Interface to interact with the `episodes` table. */
@Dao
abstract class EpisodeDao {

    // TODO: Test code.
    @Query("DELETE FROM episodes")
    abstract suspend fun clear()

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

    /** Update episode state using [EpisodeEntityState] data class. */
    @Update(entity = EpisodeEntity::class)
    abstract suspend fun update(episodeState: EpisodeEntityState)

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



    // Helper methods start.



    // Helper methods end.
}