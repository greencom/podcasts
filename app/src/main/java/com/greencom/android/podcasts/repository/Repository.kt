package com.greencom.android.podcasts.repository

import android.content.Context
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.asPodcastEntities
import com.greencom.android.podcasts.data.createAttrs
import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.database.PodcastLocalAttrs
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.utils.State
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
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
    @ApplicationContext private val appContext: Context,
    private val listenApi: ListenApiService,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val genreDao: GenreDao,
) {

    /**
     * Get a list of the best podcasts for a given genre ID. The result of the process
     * is passed directly to the given [state]. If the `podcast_table` contains the
     * appropriate list, return it. Otherwise, try to fetch best podcasts from ListenAPI
     * service and insert them into the `podcast_table`. If fetching failed, pass
     * the error to the given [state].
     *
     * @param genreId the ID of the genre to get the best podcasts for.
     * @param state list's state that function will pass values in.
     */
    suspend fun getBestPodcasts(genreId: Int, state: MutableStateFlow<State>) {
        val podcasts = podcastDao.getBestPodcasts(genreId)
        if (podcasts.isNotEmpty()) {
            state.value = State.Success(podcasts)
        } else {
            try {
                val response = listenApi.getBestPodcasts(genreId)
                podcastDao.insert(response.asPodcastEntities())
                // Create corresponding entries in the 'podcast_local_table'.
                podcastDao.insertAttrs(response.createAttrs())
                state.value = State.Success(podcastDao.getBestPodcasts(genreId))
            } catch (e: IOException) {
                state.value = State.Error(e)
            } catch (e: HttpException) {
                state.value = State.Error(e)
            }
        }
    }

    /**
     * Update the best podcasts for a given genre ID. This is done by fetching the best
     * podcasts from ListenAPI. If fetching is success, set the new list into a
     * [podcastsState] passed from ViewModel and set the success message into the [toastMessage].
     * Otherwise, set the error message into the [toastMessage].
     *
     * @param genreId Genre ID to update the best podcasts for.
     * @param podcastsState `MutableStateFlow<State>` to which the updated podcasts
     *                      will be set.
     * @param toastMessage `MutableStateFlow<String>` to which the toast message will
     *                     be set.
     * @param isRefreshing MutableStateFlow<Boolean>` to maintain the refreshing
     *                     state of the swipe-to-refresh.
     */
    suspend fun updateBestPodcasts(
        genreId: Int,
        podcastsState: MutableStateFlow<State>,
        toastMessage: MutableStateFlow<String>,
        isRefreshing: MutableStateFlow<Boolean>
    ) {
        try {
            // Fetch best podcasts from ListenAPI.
            val response = listenApi.getBestPodcasts(genreId)

            // Get the actual best podcasts and update them to exclude from the best.
            val deprecated = podcastDao.getBestPodcasts(genreId)
                .asPodcastEntities(Podcast.NOT_IN_BEST)
            podcastDao.update(deprecated)

            // Insert the fetched best podcasts to the database and return them via state.
            podcastDao.insert(response.asPodcastEntities())
            podcastDao.insertAttrs(response.createAttrs())

            // Update states.
            podcastsState.value = State.Success(podcastDao.getBestPodcasts(genreId))
            toastMessage.value = appContext.resources.getString(R.string.explore_podcasts_updated)
            isRefreshing.value = false
        } catch (e: IOException) {
            toastMessage.value = appContext.resources.getString(R.string.explore_something_went_wrong)
            isRefreshing.value = false
        } catch (e: HttpException) {
            toastMessage.value = appContext.resources.getString(R.string.explore_something_went_wrong)
            isRefreshing.value = false
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