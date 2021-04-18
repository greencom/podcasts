package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.data.domain.toDatabase
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.network.toDatabase
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
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
    override fun getBestPodcasts(
        genreId: Int,
        dispatcher: CoroutineDispatcher
    ): Flow<State<List<PodcastShort>>> = channelFlow {
        send(State.Loading)
        podcastDao.getBestPodcastsFlow(genreId).collect { podcasts ->
            // Return from the database, if it contains the appropriate podcasts.
            if (podcasts.isNotEmpty()) {
                send(State.Success(podcasts))
            } else {
                // Otherwise, fetch the best podcasts from ListenAPI and insert them into db.
                try {
                    val response = withContext(dispatcher) {
                        listenApi.getBestPodcasts(genreId)
                    }
                    podcastDao.insertWithGenre(response.toDatabase())
                } catch (e: IOException) {
                    send(State.Error(e))
                } catch (e: HttpException) {
                    send(State.Error(e))
                }
            }
        }
    }

    override suspend fun fetchBestPodcasts(
        genreId: Int,
        dispatcher: CoroutineDispatcher
    ): State<Unit> {
        return try {
            val response = withContext(dispatcher) {
                listenApi.getBestPodcasts(genreId)
            }
            podcastDao.insertWithGenre(response.toDatabase())
            State.Success(Unit)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }

    override suspend fun refreshBestPodcasts(
        genreId: Int,
        dispatcher: CoroutineDispatcher
    ): State<Unit> {
        return try {
            // Fetch a new list from ListenAPI.
            val newList = withContext(dispatcher) {
                listenApi.getBestPodcasts(genreId).toDatabase()
            }
            // Get the current list from the database.
            val currentList = podcastDao.getBestPodcasts(genreId)
            // Filter old podcasts.
            val newIds = newList.map { it.id }
            val oldList = currentList.filter { it.id !in newIds }.toDatabase(Podcast.NO_GENRE_ID)
            // Update old podcasts and insert new ones.
            podcastDao.insertWithGenre(newList)
            podcastDao.update(oldList)
            State.Success(Unit)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }
}