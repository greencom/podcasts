package com.greencom.android.podcasts.data.database

import androidx.room.*
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.data.domain.PodcastWithEpisodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Interface to interact with the `podcasts` and `podcasts_temp` tables.
 * Use [insert] and [insertWithGenre] methods to safely insert and update
 * podcasts in the `podcasts` table.
 */
@Dao
abstract class PodcastDao {

    /** Clears the whole `podcasts` table. */
    @Query("DELETE FROM podcasts")
    abstract suspend fun clear()

    /**
     * Insert a [PodcastEntityPartial] object into the `podcasts` table. This method is
     * completely safe in cases of conflicts because it uses a temporary `podcasts_temp`
     * table under the hood to calculate the diff between the temporary table and the main
     * table and only inserts the podcasts that are missing in the `podcasts` table.
     * After insertions, this method updates the `podcasts` table with the given podcast, so
     * that podcasts that were already in the `podcasts` table will be updated with the
     * new data.
     */
    @Transaction
    open suspend fun insert(podcast: PodcastEntityPartial) {
        insertPartialToTemp(podcast)
        merge()
        updatePartial(podcast)
        clearTemp()
    }

    /**
     * Insert a list of [PodcastEntityPartial] objects into the `podcasts` table. This method is
     * completely safe in cases of conflicts because it uses a temporary `podcasts_temp`
     * table under the hood to calculate the diff between the temporary table and the main
     * table and only inserts the podcasts that are missing in the `podcasts` table.
     * After insertions, this method updates the `podcasts` table with the given podcasts, so
     * that podcasts that were already in the `podcasts` table will be updated with the
     * new data.
     */
    @Transaction
    open suspend fun insert(podcasts: List<PodcastEntityPartial>) {
        insertPartialToTemp(podcasts)
        merge()
        updatePartial(podcasts)
        clearTemp()
    }

    /**
     * Insert a [PodcastEntityPartialWithGenre] object into the `podcasts` table. This method is
     * completely safe in cases of conflicts because it uses a temporary `podcasts_temp`
     * table under the hood to calculate the diff between the temporary table and the main
     * table and only inserts the podcasts that are missing in the `podcasts` table.
     * After insertions, this method updates the `podcasts` table with the given podcast, so
     * that podcasts that were already in the `podcasts` table will be updated with the
     * new data.
     */
    @Transaction
    open suspend fun insertWithGenre(podcast: PodcastEntityPartialWithGenre) {
        insertPartialWithGenreToTemp(podcast)
        merge()
        updatePartialWithGenre(podcast)
        clearTemp()
    }

    /**
     * Insert a list of [PodcastEntityPartialWithGenre] objects into the `podcasts` table.
     * This method is completely safe in cases of conflicts because it uses a temporary
     * `podcasts_temp` table under the hood to calculate the diff between the temporary table
     * and the main table and only inserts the podcasts that are missing in the `podcasts` table.
     * After insertions, this method updates the `podcasts` table with the given podcasts, so
     * that podcasts that were already in the `podcasts` table will be updated with the
     * new data.
     */
    @Transaction
    open suspend fun insertWithGenre(podcasts: List<PodcastEntityPartialWithGenre>) {
        insertPartialWithGenreToTemp(podcasts)
        merge()
        updatePartialWithGenre(podcasts)
        clearTemp()
    }

    /** Update the existing entry in the `podcasts` table with a given [PodcastEntity]. */
    @Update
    abstract suspend fun update(podcast: PodcastEntity)

    /** Update the existing entries in the `podcasts` table with a given [PodcastEntity] list. */
    @Update
    abstract suspend fun update(podcasts: List<PodcastEntity>)

    /** Update the existing entry in the `podcasts` table with a given [Podcast]. */
    @Update(entity = PodcastEntity::class)
    abstract suspend fun updateWithPodcast(podcast: Podcast)

    /** Update the existing entries in the `podcasts` table with a given [Podcast] list. */
    @Update(entity = PodcastEntity::class)
    abstract suspend fun updateWithPodcast(podcasts: List<Podcast>)

    /** Update the existing entry in the `podcasts` table with a given [PodcastShort]. */
    @Update(entity = PodcastEntity::class)
    abstract suspend fun updateWithPodcastShort(podcast: PodcastShort)

    /** Update the existing entries in the `podcasts` table with a given [PodcastShort] list. */
    @Update(entity = PodcastEntity::class)
    abstract suspend fun updateWithPodcastShort(podcast: List<PodcastShort>)

    /** Update subscription to a [Podcast] with given [PodcastSubscription] object. */
    @Update(entity = PodcastEntity::class)
    abstract suspend fun updateSubscription(podcastSubscription: PodcastSubscription)

    /** Get the update date of the podcast by ID. */
    @Query("SELECT update_date FROM podcasts WHERE id = :id")
    abstract suspend fun getUpdateDate(id: String): Long?

    /** Get the date of the latest published episode of the podcast by ID. */
    @Query("SELECT latest_pub_date FROM podcasts WHERE id = :id")
    abstract suspend fun getLatestPubDate(id: String): Long?

    /** Get the date of the earliest published episode of the podcast by ID. */
    @Query("SELECT earliest_pub_date FROM podcasts WHERE id = :id")
    abstract suspend fun getEarliestPubDate(id: String): Long?

    /**
     * Get a Flow with a [PodcastWithEpisodes] object for a given podcast ID. No need to apply
     * [distinctUntilChanged] function since it is already done under the hood.
     */
    fun getPodcastWithEpisodesFlow(id: String): Flow<PodcastWithEpisodes?> =
        getPodcastWithEpisodesFlowRaw(id).distinctUntilChanged()

    /**
     * Get a Flow with a [PodcastShort] list of the best podcasts for a given genre ID.
     * No need to apply [distinctUntilChanged] function since it is already done under
     * the hood.
     */
    fun getBestPodcastsFlow(genreId: Int): Flow<List<PodcastShort>> =
        getBestPodcastsFlowRaw(genreId).distinctUntilChanged()

    /**
     * Get a Flow with a list of subscriptions represented by [PodcastShort]. No need to
     * apply [distinctUntilChanged] function since it is already done under the hood.
     */
    fun getSubscriptionsFlow(): Flow<List<PodcastShort>> =
        getSubscriptionsFlowRaw().distinctUntilChanged()





    // Helper methods start.

    /**
     * Insert a [PodcastEntityPartial] object into the `podcasts_temp` table.
     * Do not forget to [merge] `podcasts_temp` table with `podcasts` afterwards.
     *
     * Use [insert] and [insertWithGenre] methods directly to safely insert
     * and update podcasts in the `podcasts` table.
     */
    @Insert(entity = PodcastEntityTemp::class)
    protected abstract suspend fun insertPartialToTemp(podcast: PodcastEntityPartial)

    /**
     * Insert a list of [PodcastEntityPartial] objects into the `podcasts_temp` table.
     * Do not forget to [merge] `podcasts_temp` table with `podcasts` afterwards.
     *
     * Use [insert] and [insertWithGenre] methods directly to safely insert
     * and update podcasts in the `podcasts` table.
     */
    @Insert(entity = PodcastEntityTemp::class)
    protected abstract suspend fun insertPartialToTemp(podcasts: List<PodcastEntityPartial>)

    /**
     * Insert a [PodcastEntityPartialWithGenre] object into the `podcasts_temp` table.
     * Do not forget to [merge] `podcasts_temp` table with `podcasts` afterwards.
     *
     * Use [insert] and [insertWithGenre] methods directly to safely insert
     * and update podcasts in the `podcasts` table.
     */
    @Insert(entity = PodcastEntityTemp::class)
    protected abstract suspend fun insertPartialWithGenreToTemp(podcast: PodcastEntityPartialWithGenre)

    /**
     * Insert a list of [PodcastEntityPartialWithGenre] objects into the `podcasts_temp` table.
     * Do not forget to [merge] `podcasts_temp` table with `podcasts` afterwards.
     *
     * Use [insert] and [insertWithGenre] methods directly to safely insert
     * and update podcasts in the `podcasts` table.
     */
    @Insert(entity = PodcastEntityTemp::class)
    protected abstract suspend fun insertPartialWithGenreToTemp(podcasts: List<PodcastEntityPartialWithGenre>)

    /**
     * Merge `podcasts` and `podcasts_temp` tables. All entries missing in the `podcasts`
     * table will be added from the `podcasts_temp` table. Do not forget to [clearTemp]
     * afterwards to keep `podcasts_temp` empty.
     *
     * Use [insert] and [insertWithGenre] methods directly to safely insert
     * and update podcasts in the `podcasts` table.
     */
    @Query("""
        INSERT INTO podcasts (id, title, description, image, publisher, explicit_content,
            episode_count, latest_pub_date, earliest_pub_date, subscribed, genre_id, update_date)
        SELECT t.id, t.title, t.description, t.image, t.publisher, t.explicit_content,
            t.episode_count, t.latest_pub_date, t.earliest_pub_date, t.subscribed, t.genre_id,
            t.update_date
        FROM podcasts_temp t
        LEFT JOIN podcasts ON t.id = podcasts.id
        WHERE podcasts.id IS NULL
    """)
    protected abstract suspend fun merge()

    /**
     * Update the existing entry in the `podcasts` table with a given [PodcastEntityPartial].
     */
    @Update(entity = PodcastEntity::class)
    protected abstract suspend fun updatePartial(podcast: PodcastEntityPartial)

    /**
     * Update the existing entries in the `podcasts` table with a given
     * [PodcastEntityPartial] list.
     */
    @Update(entity = PodcastEntity::class)
    protected abstract suspend fun updatePartial(podcasts: List<PodcastEntityPartial>)

    /**
     * Update the existing entry in the `podcasts` table with a given
     * [PodcastEntityPartialWithGenre].
     */
    @Update(entity = PodcastEntity::class)
    protected abstract suspend fun updatePartialWithGenre(podcast: PodcastEntityPartialWithGenre)

    /**
     * Update the existing entries in the `podcasts` table with a given
     * [PodcastEntityPartialWithGenre] list.
     */
    @Update(entity = PodcastEntity::class)
    protected abstract suspend fun updatePartialWithGenre(podcasts: List<PodcastEntityPartialWithGenre>)

    /** Get a podcast from the `podcasts_temp` table for a given ID. */
    @Query("""
        SELECT id, title, description, image, publisher, explicit_content, episode_count,
            latest_pub_date, earliest_pub_date, subscribed
        FROM podcasts_temp
        WHERE id = :id
    """)
    protected abstract suspend fun getPodcastFromTemp(id: String): Podcast?

    /** Clear the `podcasts_temp` table. */
    @Query("DELETE FROM podcasts_temp")
    protected abstract suspend fun clearTemp()

    /** Get a podcast for a given ID. */
    @Query("""
        SELECT id, title, description, image, publisher, explicit_content, episode_count,
            latest_pub_date, earliest_pub_date, subscribed
        FROM podcasts
        WHERE id = :id
    """)
    protected abstract suspend fun getPodcast(id: String): Podcast?

    /**
     * Get a Flow with a [PodcastWithEpisodes] object for a given podcast ID.
     * Use [getPodcastWithEpisodesFlow] with applied [distinctUntilChanged] function instead.
     */
    @Transaction
    @Query("""
        SELECT id, title, description, image, publisher, explicit_content, episode_count,
            latest_pub_date, earliest_pub_date, subscribed
        FROM podcasts
        WHERE id = :id
    """)
    protected abstract fun getPodcastWithEpisodesFlowRaw(id: String): Flow<PodcastWithEpisodes?>

    /**
     * Get a Flow with a [PodcastShort] list of the best podcasts for a given genre ID. Use
     * [getBestPodcastsFlow] with applied [distinctUntilChanged] function instead.
     */
    @Query("""
        SELECT id, title, description, image, publisher, explicit_content, subscribed, genre_id
        FROM podcasts
        WHERE genre_id = :genreId
    """)
    protected abstract fun getBestPodcastsFlowRaw(genreId: Int): Flow<List<PodcastShort>>

    /**
     * Get a Flow with a list of subscriptions represented by [PodcastShort]. Use
     * [getSubscriptionsFlow] with applied [distinctUntilChanged] function instead.
     */
    @Query("""
        SELECT id, title, description, image, publisher, explicit_content, subscribed, genre_id
        FROM podcasts
        WHERE subscribed = 1
        ORDER BY latest_pub_date DESC
    """)
    protected abstract fun getSubscriptionsFlowRaw(): Flow<List<PodcastShort>>

    // Helper methods end.
}