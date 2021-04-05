package com.greencom.android.podcasts.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/** Interface to interact with the `podcasts` and `podcasts_temp` tables. */
@Dao
abstract class PodcastDao {

    /** Insert a [PodcastEntityPartial] object into the `podcasts_temp` table. */
    @Insert(entity = PodcastEntityTemp::class)
    abstract fun insertPartial(podcast: PodcastEntityPartial)

    /** Insert a list of [PodcastEntityPartial] objects into the `podcasts_temp` table. */
    @Insert(entity = PodcastEntityTemp::class)
    abstract fun insertPartial(podcasts: List<PodcastEntityPartial>)

    /** Insert a [PodcastEntityPartialWithGenre] object into the `podcasts_temp` table. */
    @Insert(entity = PodcastEntityTemp::class)
    abstract fun insertPartialWithGenre(podcast: PodcastEntityPartialWithGenre)

    /** Insert a list of [PodcastEntityPartialWithGenre] objects into the `podcasts_temp` table. */
    @Insert(entity = PodcastEntityTemp::class)
    abstract fun insertPartialWithGenre(podcasts: List<PodcastEntityPartialWithGenre>)

    /**
     * Merge `podcasts` and `podcasts_temp` tables. All entries missing in the `podcasts`
     * table will be added from the `podcasts_temp` table.
     */
    @Query(
        "INSERT INTO podcasts " +
                "SELECT temp.id, temp.title, temp.description, temp.image, temp.publisher, " +
                "temp.explicit_content, temp.episode_count, temp.latest_pub_date, " +
                "temp.subscribed, temp.genre_id " +
                "FROM podcasts_temp temp " +
                "LEFT JOIN podcasts ON temp.id = podcasts.id " +
                "WHERE podcasts.id IS null"
    )
    abstract fun merge()

    /** Update the existing entry in the `podcasts` table with a given [PodcastEntity]. */
    @Update
    abstract fun update(podcast: PodcastEntity)

    /**
     * Update the existing entry in the `podcasts` table with a given [PodcastEntityPartial].
     */
    @Update(entity = PodcastEntity::class)
    abstract fun updatePartial(podcast: PodcastEntityPartial)

    /**
     * Update the existing entries in the `podcasts` table with a given
     * [PodcastEntityPartial] list.
     */
    @Update(entity = PodcastEntity::class)
    abstract fun updatePartial(podcasts: List<PodcastEntityPartial>)

    /**
     * Update the existing entry in the `podcasts` table with a given
     * [PodcastEntityPartialWithGenre].
     */
    @Update(entity = PodcastEntity::class)
    abstract fun updatePartialWithGenre(podcast: PodcastEntityPartialWithGenre)

    /**
     * Update the existing entries in the `podcasts` table with a given
     * [PodcastEntityPartialWithGenre] list.
     */
    @Update(entity = PodcastEntity::class)
    abstract fun updatePartialWithGenre(podcasts: List<PodcastEntityPartialWithGenre>)

    /** Clear the `podcasts_temp` table. */
    @Query("DELETE FROM podcasts_temp")
    abstract fun clearTemp()
}

/** Interface to interact with the `episodes` table. */
@Dao
abstract class EpisodeDao

/** Interface to interact with the `genres` table. */
@Dao
abstract class GenreDao