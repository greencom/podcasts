package com.greencom.android.podcasts.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GenreEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    /** Data access object for the `genres` table. */
    abstract fun genreDao(): GenreDao
}