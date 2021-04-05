package com.greencom.android.podcasts.data.database

import androidx.room.Dao

/** Interface to interact with the `podcasts` table. */
@Dao
abstract class PodcastDao

/** Interface to interact with the `episodes` table. */
@Dao
abstract class EpisodeDao

/** Interface to interact with the `genres` table. */
@Dao
abstract class GenreDao