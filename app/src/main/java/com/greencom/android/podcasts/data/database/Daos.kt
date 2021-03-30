package com.greencom.android.podcasts.data.database

import androidx.room.*
import com.greencom.android.podcasts.data.domain.Podcast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Interface to interact with `podcast_table` table. */
@Dao
interface PodcastDao {

    /**
     * Insert the given [PodcastEntity] object into the 'podcast_table'. Use [insertWithAttrs]
     * method instead.
     *
     * If you only want to update the existing entry, consider using [update] method.
     *
     * [OnConflictStrategy.REPLACE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: PodcastEntity)

    /**
     * Insert the given [PodcastEntity] list into the `podcast_table`. Use [insertWithAttrs]
     * method instead.
     *
     * If you only want to update the existing entries, consider using [update] method.
     *
     * [OnConflictStrategy.REPLACE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcasts: List<PodcastEntity>)

    /**
     * Insert the given [PodcastLocalAttrs] object into the 'podcast_local_table'.
     * Use [insertWithAttrs] method instead.
     *
     * If you only want to update the existing entry, consider using [updateAttrs] method.
     *
     * [OnConflictStrategy.IGNORE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttrs(attrs: PodcastLocalAttrs)

    /**
     * Insert the given [PodcastLocalAttrs] list into the 'podcast_local_table'.
     * Use [insertWithAttrs] method instead.
     *
     * [OnConflictStrategy.IGNORE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttrs(attrsList: List<PodcastLocalAttrs>)

    /** Insert given [PodcastEntity] and [PodcastLocalAttrs] lists into the database. */
    @Transaction
    suspend fun insertWithAttrs(
        podcasts: List<PodcastEntity>,
        attrsList: List<PodcastLocalAttrs>
    ) {
        insert(podcasts)
        insertAttrs(attrsList)
    }

    /** Update the existing entry in the `podcast_table`. */
    @Update
    suspend fun update(podcast: PodcastEntity)

    /** Update the existing entries in the `podcast_table`. */
    @Update
    suspend fun update(podcasts: List<PodcastEntity>)

    /** Update the existing entry in the `podcast_local_table`. */
    @Update
    suspend fun updateAttrs(attrs: PodcastLocalAttrs)

    /** Get a [Podcast] for a given ID. */
    @Query("SELECT *, local.subscribed FROM podcast_table " +
            "INNER JOIN podcast_local_table local " +
            "WHERE podcast_table.id = :id")
    suspend fun getPodcast(id: String): Podcast?

    /** Get a list of the best podcasts for a given genre ID. */
    @Query("SELECT *, local.subscribed FROM podcast_table " +
            "INNER JOIN podcast_local_table local ON podcast_table.id = local.id " +
            "WHERE podcast_table.genre_id = :genreId")
    suspend fun getBestPodcasts(genreId: Int): List<Podcast>

    /**
     * Return a Flow with a list of the best podcasts for a given genre ID. Consider using
     * [getBestPodcastsFlowDistinctUntilChanged] which applies [distinctUntilChanged]
     * under the hood.
     */
    @Query("SELECT *, local.subscribed FROM podcast_table " +
            "INNER JOIN podcast_local_table local ON podcast_table.id = local.id " +
            "WHERE podcast_table.genre_id = :genreId")
    fun getBestPodcastsFlow(genreId: Int): Flow<List<Podcast>>

    /**
     * Return a Flow with a list of the best podcasts for a given genre ID with applied
     * [distinctUntilChanged] function.
     */
    fun getBestPodcastsFlowDistinctUntilChanged(genreId: Int) =
        getBestPodcastsFlow(genreId).distinctUntilChanged()
}

/** Interface to interact with `episode_table`. */
@Dao
interface EpisodeDao

/** Interface to interact with `genre_table`. */
@Dao
interface GenreDao {

    /**
     * Insert the given [GenreEntity] list into the `genre_table`.
     *
     * [OnConflictStrategy.IGNORE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(genres: List<GenreEntity>)

    /** Get the number of entries in the `genre_table`. */
    @Query("SELECT count(*) FROM genre_table")
    suspend fun getSize(): Int
}