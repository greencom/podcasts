package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.EpisodeEntity
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.database.PodcastSubscription
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.di.DispatcherModule.IoDispatcher
import com.greencom.android.podcasts.network.*
import com.greencom.android.podcasts.utils.NO_CONNECTION
import com.greencom.android.podcasts.utils.SortOrder
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import okio.IOException
import retrofit2.HttpException
import timber.log.Timber
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

    override suspend fun updateSubscription(podcastId: String, subscribed: Boolean) {
        podcastDao.updateSubscription(PodcastSubscription(podcastId, subscribed))
    }

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

    override suspend fun fetchPodcast(id: String): State<Unit> {
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

    override fun getEpisodes(id: String): Flow<List<Episode>> {
        return episodeDao.getEpisodesFlow(id)
    }

    // TODO: Get rid of boilerplate code. Write comments.
    override suspend fun fetchEpisodes(id: String, sortOrder: SortOrder): Flow<State<Unit>> = flow {

        if (sortOrder == SortOrder.RECENT_FIRST) {
            Timber.d("Fetching for 'recent first'")
            val latestPubDate: Long
            val earliestPubDate: Long
            val recentEpisodes: List<EpisodeEntity>
            try {
                val response = withContext(ioDispatcher) {
                    listenApi.getPodcast(id, null, SortOrder.RECENT_FIRST.value)
                }
                podcastDao.insert(response.podcastToDatabase())
                latestPubDate = response.latestPubDate
                earliestPubDate = response.earliestPubDate
                recentEpisodes = response.episodesToDatabase()
            } catch (e: IOException) {
                emit(State.Error(Exception(NO_CONNECTION)))
                currentCoroutineContext().cancel()
                return@flow
            } catch (e: HttpException) {
                emit(State.Error(Exception(NO_CONNECTION)))
                currentCoroutineContext().cancel()
                return@flow
            }

            val latestLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(id)

            if (latestLoadedPubDate == null) {
                Timber.d("There are no episodes, fetch all")
                emit(State.Loading)
                episodeDao.insert(recentEpisodes)
                val result = fetchEpisodesSequentially(
                    id,
                    recentEpisodes.last().date,
                    earliestPubDate,
                    SortOrder.RECENT_FIRST
                )
                if (result is State.Error) {
                    emit(result)
                    currentCoroutineContext().cancel()
                    return@flow
                }
            } else {
                if (latestLoadedPubDate == latestPubDate) {
                    Timber.d("Latest episodes are loaded")
                    emit(State.Loading)
                    val earliestLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(id)!!

                    if (earliestLoadedPubDate != earliestPubDate) {
                        Timber.d("Earliest episodes are not loaded, fetch them")
                        val result = fetchEpisodesSequentially(
                            id,
                            earliestLoadedPubDate,
                            earliestPubDate,
                            SortOrder.RECENT_FIRST
                        )
                        if (result is State.Error) {
                            emit(result)
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                } else {
                    Timber.d("Latest episodes are not loaded, fetch them")
                    emit(State.Loading)
                    var result = fetchEpisodesSequentially(
                        id,
                        latestLoadedPubDate,
                        latestPubDate,
                        SortOrder.OLDEST_FIRST
                    )
                    if (result is State.Error) {
                        emit(result)
                        currentCoroutineContext().cancel()
                        return@flow
                    }

                    val earliestLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(id)!!

                    if (earliestLoadedPubDate != earliestPubDate) {
                        Timber.d("Earliest episodes are not loaded, fetch them")
                        result = fetchEpisodesSequentially(
                            id,
                            earliestLoadedPubDate,
                            earliestPubDate,
                            SortOrder.RECENT_FIRST
                        )
                        if (result is State.Error) {
                            emit(result)
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                }
            }
        }

        if (sortOrder == SortOrder.OLDEST_FIRST) {
            Timber.d("Fetching for 'oldest first'")
            val latestPubDate: Long
            val earliestPubDate: Long
            val earliestEpisodes: List<EpisodeEntity>
            try {
                val response = withContext(ioDispatcher) {
                    listenApi.getPodcast(id, null, SortOrder.OLDEST_FIRST.value)
                }
                podcastDao.insert(response.podcastToDatabase())
                latestPubDate = response.latestPubDate
                earliestPubDate = response.earliestPubDate
                earliestEpisodes = response.episodesToDatabase()
            } catch (e: IOException) {
                emit(State.Error(Exception(NO_CONNECTION)))
                currentCoroutineContext().cancel()
                return@flow
            } catch (e: HttpException) {
                emit(State.Error(Exception(NO_CONNECTION)))
                currentCoroutineContext().cancel()
                return@flow
            }

            val earliestLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(id)

            if (earliestLoadedPubDate == null) {
                Timber.d("There are no episodes, fetch all")
                emit(State.Loading)
                episodeDao.insert(earliestEpisodes)
                val result = fetchEpisodesSequentially(
                    id,
                    earliestEpisodes.last().date,
                    latestPubDate,
                    SortOrder.OLDEST_FIRST
                )
                if (result is State.Error) {
                    emit(result)
                    currentCoroutineContext().cancel()
                    return@flow
                }
            } else {
                if (earliestLoadedPubDate == earliestPubDate) {
                    Timber.d("Earliest episodes are loaded")
                    val latestLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(id)!!

                    if (latestLoadedPubDate != latestPubDate) {
                        Timber.d("Latest episodes are not loaded, fetch them")
                        emit(State.Loading)
                        val result = fetchEpisodesSequentially(
                            id,
                            latestLoadedPubDate,
                            latestPubDate,
                            SortOrder.OLDEST_FIRST
                        )
                        if (result is State.Error) {
                            emit(result)
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                } else {
                    Timber.d("Earliest episodes are not loaded, fetch them")
                    emit(State.Loading)
                    var result = fetchEpisodesSequentially(
                        id,
                        earliestLoadedPubDate,
                        earliestPubDate,
                        SortOrder.RECENT_FIRST
                    )
                    if (result is State.Error) {
                        emit(result)
                        currentCoroutineContext().cancel()
                        return@flow
                    }

                    val latestLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(id)!!

                    if (latestLoadedPubDate != latestPubDate) {
                        Timber.d("Latest episodes are not loaded, fetch them")
                        result = fetchEpisodesSequentially(
                            id,
                            latestLoadedPubDate,
                            latestPubDate,
                            SortOrder.OLDEST_FIRST
                        )
                        if (result is State.Error) {
                            emit(result)
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                }
            }
        }

        Timber.d("Complete")
        emit(State.Success(Unit))
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

    // TODO
    private suspend fun fetchEpisodesSequentially(
        id: String,
        startPubDate: Long,
        endPubDate: Long,
        sortOrder: SortOrder
    ): State<Unit> {
        var nextEpisodePubDate = startPubDate
        return try {
            withContext(ioDispatcher) {
                while (nextEpisodePubDate != endPubDate) {
                    val response = listenApi.getPodcast(id, nextEpisodePubDate, sortOrder.value)
                    episodeDao.insert(response.episodesToDatabase())
                    nextEpisodePubDate = response.nextEpisodePubDate
                    Timber.d("${sortOrder.name}: ${response.episodes.size} more episodes loaded with")
                }
            }
            State.Success(Unit)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }
}