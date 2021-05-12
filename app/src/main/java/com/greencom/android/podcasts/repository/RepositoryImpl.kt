package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.EpisodeEntity
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.database.PodcastSubscription
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.di.DispatcherModule.IoDispatcher
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.network.episodesToDatabase
import com.greencom.android.podcasts.network.podcastToDatabase
import com.greencom.android.podcasts.network.toDatabase
import com.greencom.android.podcasts.utils.SortOrder
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : Repository {

    @ExperimentalCoroutinesApi
    override fun getPodcast(id: String): Flow<State<Podcast>> = channelFlow {
        send(State.Loading)
        podcastDao.getPodcastFlow(id).collectLatest { podcast ->
            // If the database already contains the appropriate podcast, return it.
            if (podcast != null) {
                send(State.Success(podcast))
            } else {
                // Otherwise, fetch the podcast from ListenAPI and insert into the database.
                val result = fetchPodcast(id)
                if (result is State.Error) send(State.Error(result.exception))
            }
        }
    }

    override fun getEpisodes(id: String): Flow<List<Episode>> {
        return episodeDao.getEpisodesFlow(id)
    }

    override suspend fun fetchRecentEpisodes(id: String) {
        var latestPubDate: Long = 0
        var latestEpisodes = listOf<EpisodeEntity>()
        try {
            val response = withContext(ioDispatcher) {
                listenApi.getPodcast(id, null, SortOrder.RECENT_FIRST.value)
            }
            podcastDao.insert(response.podcastToDatabase())
            latestPubDate = response.latestPubDate
            latestEpisodes = response.episodesToDatabase()
        } catch (e: Exception) {  }

        var latestLoadedEpisodePubDate = episodeDao.getLatestLoadedEpisodePubDate(id)

        if (latestLoadedEpisodePubDate == null) {
            episodeDao.insert(latestEpisodes)
        } else {
            try {
                while (latestLoadedEpisodePubDate != latestPubDate) {
                    val response = withContext(ioDispatcher) {
                        listenApi.getPodcast(id, latestLoadedEpisodePubDate, SortOrder.OLDEST_FIRST.value)
                    }
                    episodeDao.insert(response.episodesToDatabase())
                    latestPubDate = response.latestPubDate
                    latestLoadedEpisodePubDate = maxOf(
                        response.episodes.first().date,
                        response.episodes.last().date
                    )
                }
            } catch (e: Exception) {  }
        }
    }

    override suspend fun fetchMoreEpisodes(id: String, nextEpisodePubDate: Long) {
        try {
            val response = withContext(ioDispatcher) {
                listenApi.getPodcast(id, nextEpisodePubDate)
            }
            episodeDao.insert(response.episodesToDatabase())
        } catch (e: Exception) {  }
    }

    override suspend fun updateSubscription(podcastId: String, subscribed: Boolean) {
        podcastDao.updateSubscription(PodcastSubscription(podcastId, subscribed))
    }

    @ExperimentalCoroutinesApi
    override fun getBestPodcasts(genreId: Int): Flow<State<List<PodcastShort>>> = channelFlow {
        send(State.Loading)
        podcastDao.getBestPodcastsFlow(genreId).collectLatest { podcasts ->
            // Return from the database, if it contains the appropriate podcasts.
            if (podcasts.isNotEmpty()) {
                send(State.Success(podcasts))
            } else {
                // Otherwise, fetch the best podcasts from ListenAPI and insert them into the db.
                val result = fetchBestPodcasts(genreId)
                if (result is State.Error) send(State.Error(result.exception))
            }
        }
    }

    override suspend fun refreshBestPodcasts(
        genreId: Int,
        currentList: List<PodcastShort>
    ): State<Unit> {
        return try {
            // Fetch a new list from ListenAPI.
            val newList = withContext(ioDispatcher) {
                listenApi.getBestPodcasts(genreId).toDatabase()
            }
            // Filter old podcasts.
            val newIds = newList.map { it.id }
            val oldList = currentList
                .filter { it.id !in newIds }
                .map { it.copy(genreId = Podcast.NO_GENRE_ID) }
            // Update old podcasts and insert new ones.
            podcastDao.insertWithGenre(newList)
            podcastDao.updateWithPodcastShort(oldList)
            State.Success(Unit)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }

    override suspend fun fetchBestPodcasts(genreId: Int): State<Unit> {
        return try {
            val response = withContext(ioDispatcher) {
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

    // TODO
    private suspend fun fetchPodcast(id: String): State<Unit> {
        return try {
            val response = withContext(ioDispatcher) {
                listenApi.getPodcast(id, null)
            }
            podcastDao.insert(response.podcastToDatabase())
            State.Success(Unit)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }
}