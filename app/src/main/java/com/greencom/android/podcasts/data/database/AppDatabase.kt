package com.greencom.android.podcasts.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/** App database that provides DAO objects to get access to the database tables. */
@Database(
    entities = [PodcastEntity::class,
        PodcastLocalAttrs::class,
        EpisodeEntity::class,
        GenreEntity::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {

    /** Data access object for the `podcast_table` table. */
    abstract fun podcastDao(): PodcastDao

    /** Data access object for the `episode_table` table. */
    abstract fun episodeDao(): EpisodeDao

    /** Data access object for the `genre_table` table. */
    abstract fun genreDao(): GenreDao
}