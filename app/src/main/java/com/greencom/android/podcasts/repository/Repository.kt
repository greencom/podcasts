package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.asPodcastEntities
import com.greencom.android.podcasts.data.createAttrs
import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.database.PodcastLocalAttrs
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.utils.State
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App repository. Provides access to the ListenAPI network service via [ListenApiService]
 * and the app database tables via appropriate DAO objects.
 */
@Singleton
class Repository @Inject constructor(
    private val listenApi: ListenApiService,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val genreDao: GenreDao,
) {

    /**
     * Get a list of the best podcasts for a given genre ID. If there are the
     * appropriate podcasts in the database, return them. Otherwise, try to fetch
     * the best podcasts from ListenAPI.
     *
     * Returns the result as a [State] object.
     */
    suspend fun getBestPodcasts(genreId: Int): State {
        val podcasts = podcastDao.getBestPodcasts(genreId)
        // Try to get from the database.
        if (podcasts.isNotEmpty()) {
            return State.Success(podcasts)
        }
        // Try to fetch from ListenAPI.
        return try {
            val response = listenApi.getBestPodcasts(genreId)
            podcastDao.insert(response.asPodcastEntities())
            // Create corresponding entries in the 'podcast_local_table'.
            podcastDao.insertAttrs(response.createAttrs())
            State.Success(podcastDao.getBestPodcasts(genreId))
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }

    /**
     * Update the best podcasts for a given genre ID from ListenAPI. Returns the result
     * as a [State] object.
     */
    suspend fun updateBestPodcasts(genreId: Int): State {
        return try {
            // Try to fetch the best podcasts from ListenAPI.
            val response = listenApi.getBestPodcasts(genreId)

            // Get the actual best podcasts and update them to exclude from the best.
            val deprecated = podcastDao.getBestPodcasts(genreId)
                .asPodcastEntities(Podcast.NOT_IN_BEST)
            podcastDao.update(deprecated)

            // Insert the fetched best podcasts and their attrs into the database.
            podcastDao.insert(response.asPodcastEntities())
            podcastDao.insertAttrs(response.createAttrs())
            State.Success(podcastDao.getBestPodcasts(genreId))
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }

    /**
     * Update the subscription on a given podcast with a given value using
     * the corresponding [PodcastLocalAttrs] entry.
     */
    suspend fun updateSubscription(podcast: Podcast, subscribed: Boolean) {
        podcast.subscribed = subscribed
        podcastDao.updateAttrs(PodcastLocalAttrs(podcast.id, subscribed))
    }
}