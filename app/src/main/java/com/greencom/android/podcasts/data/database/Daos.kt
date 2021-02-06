package com.greencom.android.podcasts.data.database

import androidx.room.*

/** TODO: Documentation, Test */
@Dao
interface GenreDao {

    /** Insert the [GenreEntity] list into the `genres` table. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(genres: List<GenreEntity>)

    /** Update the `genres` table with the given [GenreEntity] list. */
    @Update
    suspend fun updateAll(genres: List<GenreEntity>)

    /** Get a [GenreEntity] list that includes only top level genres. */
    @Query("SELECT * FROM genres WHERE parentId = 67")
    suspend fun getTopLevelOnly(): List<GenreEntity>?

    /** Get a [GenreEntity] list that includes all genres. */
    @Query("SELECT * FROM genres")
    suspend fun getAll(): List<GenreEntity>?

    /** Clear whole `genres` table. */
    @Query("DELETE FROM genres")
    suspend fun deleteAll()
}
