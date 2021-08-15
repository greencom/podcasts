package com.greencom.android.podcasts.data.database

import androidx.room.*
import com.greencom.android.podcasts.data.domain.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Interface to interact with the `episodes` table. */
@Dao
abstract class EpisodeDao {

    /** Clears the whole `episodes` table. */
    @Query("DELETE FROM episodes")
    abstract suspend fun clear()

    /**
     * Insert an [EpisodeEntity] object into the database. [OnConflictStrategy.IGNORE]
     * on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(episode: EpisodeEntity)

    /**
     * Insert a list of [EpisodeEntity]s into the database. [OnConflictStrategy.IGNORE]
     * on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(episodes: List<EpisodeEntity>)

    /** Update episode state using [EpisodeEntityState] data class. */
    @Update(entity = EpisodeEntity::class)
    abstract suspend fun update(episodeState: EpisodeEntityState)

    /** Update episode-in-bookmarks state with [EpisodeEntityBookmark] data class. */
    @Update(entity = EpisodeEntity::class)
    abstract suspend fun update(episodeBookmark: EpisodeEntityBookmark)

    /** Get the number of loaded episodes for a given podcast ID. */
    @Query("SELECT COUNT(id) FROM episodes WHERE podcast_id = :podcastId")
    abstract suspend fun getEpisodeCount(podcastId: String): Int

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

    /** Returns `true` if the episode is in the bookmarks. */
    @Query("SELECT in_bookmarks FROM episodes where id = :episodeId")
    abstract suspend fun isEpisodeInBookmarks(episodeId: String): Boolean?

    /** Get episode's last position by ID. */
    @Query("SELECT position FROM episodes WHERE id = :id")
    abstract suspend fun getEpisodePosition(id: String): Long?

    /** Get an episode by ID. */
    @Query("SELECT * FROM episodes WHERE id = :id")
    abstract suspend fun getEpisode(id: String): Episode?

    /**
     * Get a Flow with an [Episode] for a given ID. No need to apply [distinctUntilChanged]
     * function since it is already done under the hood.
     */
    fun getEpisodeFlow(id: String): Flow<Episode?> = getEpisodeFlowRaw(id).distinctUntilChanged()

    /**
     * Get a Flow with a list of episodes that have been added to the bookmarks in
     * descending order of the add date. No need to apply [distinctUntilChanged] function
     * since it is already done under the hood.
     */
    fun getBookmarksFlow(): Flow<List<Episode>> = getBookmarksFlowRaw()
        .distinctUntilChanged()

    /**
     * Get a Flow with a list of episodes in progress in descending order of the last played date.
     * No need to apply [distinctUntilChanged] function since it is already done under the hood.
     */
    fun getEpisodesInProgressFlow(): Flow<List<Episode>> = getEpisodesInProgressFlowRaw()
        .distinctUntilChanged()

    /**
     * Get a Flow with a list of completed [Episode]s in descending order of end date. No need to
     * apply [distinctUntilChanged] function since it is already done under the hood.
     */
    fun getEpisodeHistoryFlow(): Flow<List<Episode>> = getEpisodeHistoryFlowRaw()
        .distinctUntilChanged()



    // Helper methods start.

    /**
     * Get a Flow with an [Episode] for a given ID. Use [getEpisodeFlow] with applied
     * [distinctUntilChanged] function instead.
     */
    @Query("""
        SELECT id, title, description, podcast_title, publisher, image, audio, audio_length, 
            podcast_id, explicit_content, date, position, last_played_date, is_completed, 
            completion_date, in_bookmarks, added_to_bookmarks_date
        FROM episodes
        WHERE id = :id
    """)
    protected abstract fun getEpisodeFlowRaw(id: String): Flow<Episode?>

    /**
     * Get a Flow with a list of episodes that were have been added to the bookmarks in
     * descending order of the add date. Use [getBookmarksFlow] with applied [distinctUntilChanged]
     * function instead.
     */
    @Query("""
        SELECT id, title, description, podcast_title, publisher, image, audio, audio_length, 
            podcast_id, explicit_content, date, position, last_played_date, is_completed, 
            completion_date, in_bookmarks, added_to_bookmarks_date
        FROM episodes
        WHERE in_bookmarks = 1
        ORDER BY added_to_bookmarks_date DESC
    """)
    protected abstract fun getBookmarksFlowRaw(): Flow<List<Episode>>

    /**
     * Get a Flow with a list of episodes in progress in descending order of the last played date.
     * Use [getEpisodesInProgressFlow] with applied [distinctUntilChanged] function instead.
     */
    @Query("""
        SELECT id, title, description, podcast_title, publisher, image, audio, audio_length, 
            podcast_id, explicit_content, date, position, last_played_date, is_completed, 
            completion_date, in_bookmarks, added_to_bookmarks_date
        FROM episodes
        WHERE position != 0
        ORDER BY last_played_date DESC
    """)
    protected abstract fun getEpisodesInProgressFlowRaw(): Flow<List<Episode>>

    /**
     * Get a Flow with a list of completed [Episode]s in descending order of the end date. Use
     * [getEpisodeHistoryFlow] with applied [distinctUntilChanged] function instead.
     */
    @Query("""
        SELECT id, title, description, podcast_title, publisher, image, audio, audio_length, 
            podcast_id, explicit_content, date, position, last_played_date, is_completed, 
            completion_date, in_bookmarks, added_to_bookmarks_date
        FROM episodes
        WHERE is_completed = 1
        ORDER BY completion_date DESC
    """)
    protected abstract fun getEpisodeHistoryFlowRaw(): Flow<List<Episode>>

    // Helper methods end.
}