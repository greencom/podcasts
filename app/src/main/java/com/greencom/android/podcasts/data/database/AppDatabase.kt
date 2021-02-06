package com.greencom.android.podcasts.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/** TODO: Documentation */
@Database(entities = [GenreEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    /** TODO: Documentation */
    abstract fun genreDao(): GenreDao
}