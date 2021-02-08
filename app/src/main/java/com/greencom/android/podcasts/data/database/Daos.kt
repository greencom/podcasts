package com.greencom.android.podcasts.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Interface to interact with `genres` table. */
@Dao
interface GenreDao {

    /** Get the number of entries in the `genres` table. */
    @Query("SELECT count(*) FROM genres")
    suspend fun getSize(): Int

    /** Insert the given [GenreEntity] list into the `genres` table. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(genres: List<GenreEntity>)

    /**
     * Get a [GenreEntity] list that includes only genres used as the tabs
     * in the ExploreFragment TabLayout.
     */
    @Query("SELECT * FROM genres WHERE inExplore = 1")
    suspend fun getExploreOnly(): List<GenreEntity>

    /** Clear whole `genres` table. */
    @Query("DELETE FROM genres")
    suspend fun deleteAll()
}
