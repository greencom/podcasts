package com.greencom.android.podcasts.data.database

import androidx.room.Dao

/** Interface to interact with the `podcasts` table. */
@Dao
interface PodcastDao

/** Interface to interact with the `episodes` table. */
@Dao
interface EpisodeDao

/** Interface to interact with the `genres` table. */
@Dao
interface GenreDao