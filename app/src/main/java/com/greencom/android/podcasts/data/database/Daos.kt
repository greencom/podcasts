package com.greencom.android.podcasts.data.database

import androidx.room.*
import com.greencom.android.podcasts.data.domain.Podcast

/** Interface to interact with `podcast_table` table. */
@Dao
interface PodcastDao {

    /**
     * Insert the given [PodcastEntity] object into the 'podcast_table'.
     * [OnConflictStrategy.REPLACE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: PodcastEntity)

    /**
     * Insert the given [PodcastEntity] list into the `podcast_table`.
     * [OnConflictStrategy.REPLACE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcasts: List<PodcastEntity>)

    @Update
    suspend fun update(podcast: PodcastEntity)

    @Update
    suspend fun update(podcasts: List<PodcastEntity>)

    /**
     * Insert the given [PodcastLocalAttrs] object into the 'podcast_local_table'.
     * [OnConflictStrategy.IGNORE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttrs(attrs: PodcastLocalAttrs)

    /**
     * Insert the given [PodcastLocalAttrs] list into the 'podcast_local_table'.
     * [OnConflictStrategy.IGNORE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttrs(attrsList: List<PodcastLocalAttrs>)

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
}

/** Interface to interact with `episode_table`. */
@Dao
interface EpisodeDao

/** Interface to interact with `genre_table`. */
@Dao
interface GenreDao {

    /**
     * Insert the given [GenreEntity] list into the `genre_table`.
     * [OnConflictStrategy.IGNORE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(genres: List<GenreEntity>)

    /** Get the number of entries in the `genre_table`. */
    @Query("SELECT count(*) FROM genre_table")
    suspend fun getSize(): Int
}