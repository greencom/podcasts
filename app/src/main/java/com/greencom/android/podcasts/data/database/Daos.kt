package com.greencom.android.podcasts.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Interface to interact with `podcasts` table. */
@Dao
interface PodcastDao {

    /** Insert the given [PodcastEntity] list into the `podcasts` table. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcasts: List<PodcastEntity>)
}

/** Interface to interact with `episodes` table. */
@Dao
interface EpisodeDao {

}

/** Interface to interact with `genres` table. */
@Dao
interface GenreDao {

    /** Get the number of entries in the `genres` table. */
    @Query("SELECT count(*) FROM genres")
    suspend fun getSize(): Int

    /** Insert the given [GenreEntity] list into the `genres` table. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(genres: List<GenreEntity>)

    /** Get a [GenreEntity] instance by provided genre name. */
    @Query("SELECT * FROM genres WHERE name = :name")
    suspend fun getGenre(name: String): GenreEntity

    /** Clear whole `genres` table. */
    @Query("DELETE FROM genres")
    suspend fun deleteAll()
}
