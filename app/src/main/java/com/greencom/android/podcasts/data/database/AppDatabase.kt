package com.greencom.android.podcasts.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/** App database that provides DAO objects to get access to the database tables. */
@Database(
    entities = [
        PodcastEntity::class,
        PodcastEntityTemp::class,
        EpisodeEntity::class,
    ],
    version = 9
)
abstract class AppDatabase : RoomDatabase() {

    /** Data access object for the `podcasts` table. */
    abstract fun podcastDao(): PodcastDao

    /** Data access object for the `episodes` table. */
    abstract fun episodeDao(): EpisodeDao
}