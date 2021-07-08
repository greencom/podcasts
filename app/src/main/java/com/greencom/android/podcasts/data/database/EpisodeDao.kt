package com.greencom.android.podcasts.data.database

import androidx.room.*
import com.greencom.android.podcasts.data.domain.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

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

    /** Get the episode by ID. */
    @Query("SELECT * FROM episodes WHERE id = :id")
    abstract suspend fun getEpisode(id: String): Episode?

    /**
     * Get a Flow with an [Episode] for a given ID. No need to apply [distinctUntilChanged]
     * function since it is already done under the hood.
     */
    fun getEpisodeFlow(id: String): Flow<Episode?> = getEpisodeFlowRaw(id).distinctUntilChanged()

    /** Get the episode's last position by ID. */
    @Query("SELECT position FROM episodes WHERE id = :id")
    abstract suspend fun getEpisodePosition(id: String): Long?

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

    /**
     * Get a Flow with an [Episode] for a given ID. Use [getEpisodeFlow] with applied
     * [distinctUntilChanged] function instead.
     */
    @Query("""
        SELECT id, title, description, podcast_title, publisher, image, audio, audio_length, 
            podcast_id, explicit_content, date, position, is_completed, completion_date
        FROM episodes
        WHERE id = :id
    """)
    protected abstract fun getEpisodeFlowRaw(id: String): Flow<Episode?>

    // Helper methods end.
}