package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.network.toDatabase
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App [Repository] implementation. Provides access to the ListenAPI network service
 * and the app database tables.
 */
@Singleton
class RepositoryImpl @Inject constructor(
    private val listenApi: ListenApiService,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
) : Repository {

    @ExperimentalCoroutinesApi
    override fun getBestPodcasts(genreId: Int): Flow<State<List<PodcastShort>>> = channelFlow {
        podcastDao.getBestPodcastsFlow(genreId).collect { podcasts ->
            // Return from the database, if it contains the appropriate podcasts.
            if (podcasts.isNotEmpty()) {
                send(State.Success(podcasts))
            } else {
                // Otherwise, fetch the best podcasts from ListenAPI and insert them into db.
                try {
                    val response = listenApi.getBestPodcasts(genreId)
                    podcastDao.insertWithGenre(response.toDatabase())
                } catch (e: IOException) {
                    send(State.Error(e))
                } catch (e: HttpException) {
                    send(State.Error(e))
                }
            }
        }
    }
}