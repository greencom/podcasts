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

    // TODO
    fun getEpisodesFlow(id: String) = getEpisodesFlowRaw(id).distinctUntilChanged()



    // Helper methods start.

    // TODO
    @Query(
        "SELECT id, title, description, image, audio, audio_length, podcast_id, " +
                "explicit_content, date " +
                "FROM episodes " +
                "WHERE podcast_id = :id " +
                "ORDER BY date DESC")
    protected abstract fun getEpisodesFlowRaw(id: String): Flow<List<Episode>>

    // Helper methods end.
}