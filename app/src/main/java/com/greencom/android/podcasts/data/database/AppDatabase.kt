package com.greencom.android.podcasts.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/** App database that provides data access objects for the database tables. */
@Database(
    entities = [
        PodcastEntity::class,
        PodcastEntityTemp::class,
        EpisodeEntity::class,
    ],
    version = 15
)
abstract class AppDatabase : RoomDatabase() {

    /** Data access object for the `podcasts` table. */
    abstract fun podcastDao(): PodcastDao

    /** Data access object for the `episodes` table. */
    abstract fun episodeDao(): EpisodeDao
}