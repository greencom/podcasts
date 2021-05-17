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

    // TODO: Get rid of boilerplate code.
    override suspend fun fetchEpisodes(id: String, sortOrder: SortOrder): Flow<State<Unit>> = flow {
        // The published dates of the latest and earliest episodes of the podcast in ms.
        var latestPubDate: Long
        var earliestPubDate: Long

        // Algorithm for "recent first" mode.
        if (sortOrder == SortOrder.RECENT_FIRST) {
            Timber.d("Fetching has started for ${sortOrder.name}")

            // Fetch the podcast data.
            val recentEpisodes: List<EpisodeEntity>
            try {
                val response = withContext(ioDispatcher) {
                    listenApi.getPodcast(id, null, SortOrder.RECENT_FIRST.value)
                }
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
            Timber.d("Recent podcast data has fetched")

            // Get the date of the latest episode loaded to the database for this podcast.
            Timber.d("Get latestLoadedPubDate")
            val latestLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(id)
            emit(State.Loading)

            if (latestLoadedPubDate == null) {
                Timber.d("latestLoadedPubDate is null, there are no episodes")
                // If the latestLoadedPubDate is null, it means there are no episodes in the
                // database for this podcast.

                Timber.d("Insert recent fetched episodes")
                // Insert recent episodes into the database.
                episodeDao.insert(recentEpisodes)

                // Load more episodes until the nextEpisodePubDate value is equal to
                // the earliestPubDate.
                var nextEpisodePubDate = recentEpisodes.last().date
                try {
                    withContext(ioDispatcher) {
                        while (nextEpisodePubDate != earliestPubDate) {
                            val response = listenApi.getPodcast(
                                id,
                                nextEpisodePubDate,
                                SortOrder.RECENT_FIRST.value
                            )
                            episodeDao.insert(response.episodesToDatabase())
                            nextEpisodePubDate = response.nextEpisodePubDate
                            Timber.d("${response.episodes.size} more episodes loaded with nextEpisodePubDate = $nextEpisodePubDate")
                        }
                    }
                } catch (e: IOException) {
                    emit(State.Error(e))
                    currentCoroutineContext().cancel()
                    return@flow
                } catch (e: HttpException) {
                    emit(State.Error(e))
                    currentCoroutineContext().cancel()
                    return@flow
                }
            } else {
                Timber.d("latestLoadedPubDate is not null, episodes exist")
                // If the latestLoadedPubDate is not null, it means there are episodes in the
                // database for this podcast.

                // Check if the latest episode loaded to the db is still the latest for the podcast.
                if (latestLoadedPubDate == latestPubDate) {
                    Timber.d("latestLoadedPubDate == latestPubDate, recent episodes are loaded")
                    // If the latestLoadedPubDate == latestPubDate, two cases are possible:
                    // 1: If the previous load was for "recent first", episodes were downloaded
                    // sequentially from the latest one to some point. So find the oldest loaded
                    // episode and continue from there.
                    // 2: If the previous load was for "oldest first", episodes were downloaded
                    // sequentially from the oldest one to the latest one, so all episodes have
                    // been loaded already.

                    // Get the date of the oldest episode loaded to the database for this podcast.
                    val earliestLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(id)

                    // Handle the first case.
                    if (earliestLoadedPubDate != earliestPubDate) {
                        Timber.d("earliestLoadedPubDate != earliestPubDate, the earliest episodes not loaded")
                        // Load more episodes until the nextEpisodePubDate value is equal to
                        // the earliestPubDate.
                        var nextEpisodePubDate = earliestLoadedPubDate
                        try {
                            withContext(ioDispatcher) {
                                while (nextEpisodePubDate != earliestPubDate) {
                                    val response = listenApi.getPodcast(
                                        id,
                                        nextEpisodePubDate,
                                        SortOrder.RECENT_FIRST.value
                                    )
                                    episodeDao.insert(response.episodesToDatabase())
                                    nextEpisodePubDate = response.nextEpisodePubDate
                                    Timber.d("${response.episodes.size} more episodes loaded with nextEpisodePubDate = $nextEpisodePubDate")
                                }
                            }
                        } catch (e: IOException) {
                            emit(State.Error(e))
                            currentCoroutineContext().cancel()
                            return@flow
                        } catch (e: HttpException) {
                            emit(State.Error(e))
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                } else {
                    Timber.d("latestLoadedPubDate != latestPubDate, recent episodes not loaded")
                    // If the latestLoadedPubDate != latestPubDate, load the new ones at first.
                    var nextEpisodePubDate = latestLoadedPubDate
                    try {
                        withContext(ioDispatcher) {
                            while (nextEpisodePubDate != latestPubDate) {
                                // Load in reverse order from the latestLoadedPubDate.
                                // Reverse order ensures that episodes are loaded in sequence and
                                // and that there are no spaces in between.
                                val response = listenApi.getPodcast(
                                    id,
                                    nextEpisodePubDate,
                                    SortOrder.OLDEST_FIRST.value
                                )
                                episodeDao.insert(response.episodesToDatabase())
                                nextEpisodePubDate = response.nextEpisodePubDate
                                Timber.d("${response.episodes.size} more episodes loaded with nextEpisodePubDate = $nextEpisodePubDate")
                            }
                        }
                    } catch (e: IOException) {
                        emit(State.Error(e))
                        currentCoroutineContext().cancel()
                        return@flow
                    } catch (e: HttpException) {
                        emit(State.Error(e))
                        currentCoroutineContext().cancel()
                        return@flow
                    }

                    // Then continue loading from the earliest loaded date to the earliest
                    // published date of the podcast.

                    // Get the date of the earliest episode loaded to the database for this podcast.
                    val earliestLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(id)

                    if (earliestLoadedPubDate != earliestPubDate) {
                        // Load more episodes until the nextEpisodePubDate value is equal to
                        // the earliestPubDate.
                        nextEpisodePubDate = earliestLoadedPubDate
                        try {
                            withContext(ioDispatcher) {
                                while (nextEpisodePubDate != earliestPubDate) {
                                    val response = listenApi.getPodcast(
                                        id,
                                        nextEpisodePubDate,
                                        SortOrder.RECENT_FIRST.value
                                    )
                                    episodeDao.insert(response.episodesToDatabase())
                                    nextEpisodePubDate = response.nextEpisodePubDate
                                    Timber.d("${response.episodes.size} more episodes loaded with nextEpisodePubDate = $nextEpisodePubDate")
                                }
                            }
                        } catch (e: IOException) {
                            emit(State.Error(e))
                            currentCoroutineContext().cancel()
                            return@flow
                        } catch (e: HttpException) {
                            emit(State.Error(e))
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                }
            }
        }

        // Algorithm for "oldest first" mode.
        if (sortOrder == SortOrder.OLDEST_FIRST) {
            Timber.d("Fetching has started for ${sortOrder.name}")

            // Fetch the podcast data.
            val earliestEpisodes: List<EpisodeEntity>
            try {
                val response = withContext(ioDispatcher) {
                    listenApi.getPodcast(id, null, SortOrder.OLDEST_FIRST.value)
                }
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
            Timber.d("Recent podcast data has fetched")

            // Get the date of the earliest episode loaded to the database for this podcast.
            Timber.d("Get earliestLoadedPubDate")
            val earliestLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(id)
            emit(State.Loading)

            if (earliestLoadedPubDate == null) {
                Timber.d("earliestLoadedPubDate is null, there are no episodes")
                // If the earliestLoadedPubDate is null, it means there are no episodes in the
                // database for this podcast.

                Timber.d("Insert earliest fetched episodes")
                // Insert earliest episodes into the database.
                episodeDao.insert(earliestEpisodes)

                // Load more episodes until the nextEpisodePubDate value is equal to
                // the latestPubDate.
                var nextEpisodePubDate = earliestEpisodes.last().date
                try {
                    withContext(ioDispatcher) {
                        while (nextEpisodePubDate != latestPubDate) {
                            val response = listenApi.getPodcast(
                                id,
                                nextEpisodePubDate,
                                SortOrder.OLDEST_FIRST.value
                            )
                            episodeDao.insert(response.episodesToDatabase())
                            nextEpisodePubDate = response.nextEpisodePubDate
                            Timber.d("${response.episodes.size} more episodes loaded with nextEpisodePubDate = $nextEpisodePubDate")
                        }
                    }
                } catch (e: IOException) {
                    emit(State.Error(e))
                    currentCoroutineContext().cancel()
                    return@flow
                } catch (e: HttpException) {
                    emit(State.Error(e))
                    currentCoroutineContext().cancel()
                    return@flow
                }
            } else {
                Timber.d("earliestLoadedPubDate is not null, episodes exist")
                // If the earliestLoadedPubDate is not null, it means there are episodes in the
                // database for this podcast.

                // Check if the earliest episode loaded to the db is the earliest for the
                // podcast.
                if (earliestLoadedPubDate != earliestPubDate) {
                    Timber.d("earliestLoadedPubDate != earliestPubDate, the earliest episodes not loaded")
                    // Load from the earliestLoadedPubDate to the earliestPubDate in reverse order
                    // to ensure that episodes are loaded in sequence.
                    var nextEpisodePubDate = earliestLoadedPubDate
                    try {
                        withContext(ioDispatcher) {
                            while (nextEpisodePubDate != earliestPubDate) {
                                val response = listenApi.getPodcast(
                                    id,
                                    nextEpisodePubDate,
                                    SortOrder.RECENT_FIRST.value
                                )
                                episodeDao.insert(response.episodesToDatabase())
                                nextEpisodePubDate = response.nextEpisodePubDate
                                Timber.d("${response.episodes.size} more episodes loaded with nextEpisodePubDate = $nextEpisodePubDate")
                            }
                        }
                    } catch (e: IOException) {
                        emit(State.Error(e))
                        currentCoroutineContext().cancel()
                        return@flow
                    } catch (e: HttpException) {
                        emit(State.Error(e))
                        currentCoroutineContext().cancel()
                        return@flow
                    }

                    // Then, check if the latestLoadedPubDate is equal to latestPubDate.
                    // If not, load the latest episodes.
                    val latestLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(id)

                    if (latestLoadedPubDate != latestPubDate) {
                        Timber.d("latestLoadedPubDate != latestPubDate, the latest episodes not loaded")
                        nextEpisodePubDate = latestLoadedPubDate
                        try {
                            withContext(ioDispatcher) {
                                while (nextEpisodePubDate != latestPubDate) {
                                    val response = listenApi.getPodcast(
                                        id,
                                        nextEpisodePubDate,
                                        SortOrder.OLDEST_FIRST.value
                                    )
                                    episodeDao.insert(response.episodesToDatabase())
                                    nextEpisodePubDate = response.nextEpisodePubDate
                                    Timber.d("${response.episodes.size} more episodes loaded with nextEpisodePubDate = $nextEpisodePubDate")
                                }
                            }
                        } catch (e: IOException) {
                            emit(State.Error(e))
                            currentCoroutineContext().cancel()
                            return@flow
                        } catch (e: HttpException) {
                            emit(State.Error(e))
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                } else {
                    Timber.d("earliestLoadedPubDate == earliestPubDate, the earliest episodes are loaded")
                    // Load episodes from latestLoadedPubDate to the latestPubDate.
                    val latestLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(id)

                    if (latestLoadedPubDate != latestPubDate) {
                        Timber.d("latestLoadedPubDate != latestPubDate, the latest episodes not loaded")
                        var nextEpisodePubDate = latestLoadedPubDate
                        try {
                            withContext(ioDispatcher) {
                                while (nextEpisodePubDate != latestPubDate) {
                                    val response = listenApi.getPodcast(
                                        id,
                                        nextEpisodePubDate,
                                        SortOrder.OLDEST_FIRST.value
                                    )
                                    episodeDao.insert(response.episodesToDatabase())
                                    nextEpisodePubDate = response.nextEpisodePubDate
                                    Timber.d("${response.episodes.size} more episodes loaded with nextEpisodePubDate = $nextEpisodePubDate")
                                }
                            }
                        } catch (e: IOException) {
                            emit(State.Error(e))
                            currentCoroutineContext().cancel()
                            return@flow
                        } catch (e: HttpException) {
                            emit(State.Error(e))
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                }
            }
        }

        Timber.d("Fetching has finished")
        emit(State.Success(Unit))
    }

    override suspend fun fetchMoreEpisodes(id: String, nextEpisodePubDate: Long) {
        try {
            val response = withContext(ioDispatcher) {
                listenApi.getPodcast(id, nextEpisodePubDate)
            }
            episodeDao.insert(response.episodesToDatabase())
        } catch (e: Exception) {  }
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
}