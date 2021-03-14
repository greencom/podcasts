package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.utils.State
import com.greencom.android.podcasts.utils.asPodcasts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _genresState = MutableStateFlow<State>(State.Init)
    /** Represents the state of loading genres using [State] class. */
    val genresState: StateFlow<State> = _genresState

    /**
     * Get a list of the best podcasts for a given genre ID. The result of the process
     * is passed directly to the given [state]. If the `podcasts` table contains the
     * appropriate list, return it. Otherwise, try to fetch best podcasts from ListenAPI
     * service and insert them into the `podcasts` table. If fetching failed, pass
     * the error to the given [state].
     *
     * @param genreId the ID of the genre to get the best podcasts for.
     * @param state list's state that function will pass
     *              values in.
     */
    suspend fun getBestPodcasts(genreId: Int, state: MutableStateFlow<State>) {
        val podcasts = podcastDao.getBestPodcasts(genreId)
        if (podcasts.isNotEmpty()) {
            state.value = State.Success(podcasts.asPodcasts())
        } else {
            try {
                val response = listenApi.getBestPodcasts(genreId)
                podcastDao.insert(response.asPodcastEntities())
                state.value = State.Success(response.asPodcasts())
            } catch (e: IOException) {
                state.value = State.Error(e)
            } catch (e: HttpException) {
                state.value = State.Error(e)
            }
        }
    }

    /**
     * Update the subscription on the given podcast with the given value.
     * Update the existing entry in the `podcasts` table with the new one.
     */
    suspend fun updateSubscription(podcast: Podcast, newValue: Boolean) {
        podcast.inSubscriptions = newValue
        podcastDao.update(podcast.updateSubscription(newValue))
    }

    /**
     * Fetch genre list from ListenAPI and insert it into the `genres` table,
     * if the table is empty. Use [genresState] to get the state of loading process.
     */
    suspend fun updateGenres() {
        if (genreDao.getSize() == 0) {
            _genresState.value = State.Loading
            try {
                val genres = listenApi.getGenres().asGenreEntities()
                genreDao.insert(genres)
                _genresState.value = State.Success(Unit)
            } catch (e: IOException) {
                _genresState.value = State.Error(e)
            } catch (e: HttpException) {
                _genresState.value = State.Error(e)
            }
        } else {
            _genresState.value = State.Success(Unit)
        }
    }
}