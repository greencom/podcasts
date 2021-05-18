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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Limit that defines how many episodes can be loaded to the bottom of the sorted list
 * in one [RepositoryImpl.fetchEpisodes] function call.
 *
 * Note: episodes loaded to the top of the sorted list are not counted to ensure that the
 * user will see the appropriate episodes at the top of the list according to the sort order.
 */
private const val EPISODES_LIMIT = 30

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

    override suspend fun fetchPodcast(id: String): State<PodcastWrapper> {
        return try {
            val response = withContext(ioDispatcher) {
                listenApi.getPodcast(id, null)
            }
            podcastDao.insert(response.podcastToDatabase())
            State.Success(response)
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
    override suspend fun fetchEpisodes(
        id: String,
        sortOrder: SortOrder,
        isForced: Boolean
    ): Flow<State<Unit>> = flow {

        // How many episodes have been loaded.
        var episodesLoaded = 0

        // Declare 2 vars for the podcast latest and earliest pub dates and 1 var for the
        // episode list that is top for the current sort order.
        Timber.d("Fetching for $sortOrder")
        val latestPubDate: Long?
        val earliestPubDate: Long?
        var topEpisodes = listOf<EpisodeEntity>()

        if (isForced) {
            // If forced by user, update the podcast data regardless of the last update
            // time.
            Timber.d("Forced by user, update the podcast data")
            try {
                val response = withContext(ioDispatcher) {
                    listenApi.getPodcast(id, null, sortOrder.value)
                }
                podcastDao.insert(response.podcastToDatabase())
                latestPubDate = response.latestPubDate
                earliestPubDate = response.earliestPubDate
                topEpisodes = response.episodesToDatabase()
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
            // Check when the podcast data was updated.
            val updateDate = podcastDao.getUpdateDate(id)
            if (updateDate != null) {
                // If updateDate != null, there is podcast data in the database.
                val timeFromLastUpdate = System.currentTimeMillis() - updateDate
                // Check if the podcast was recently updated.
                if (timeFromLastUpdate <= TimeUnit.HOURS.toMillis(3)) {
                    // If the podcast data was recently updated, get latestPubDate and earliestPubDate
                    // from the database.
                    Timber.d("The podcast was recently updated, get latestPubDate and earliestPubDate from the database")
                    latestPubDate = podcastDao.getLatestPubDate(id)!!
                    earliestPubDate = podcastDao.getEarliestPubDate(id)!!
                } else {
                    // If the podcast data was updated more that 3 hours ago, update the podcast
                    // data from ListenAPI.
                    Timber.d("The podcast was updated more than 3 hours ago, update it")
                    try {
                        val response = withContext(ioDispatcher) {
                            listenApi.getPodcast(id, null, sortOrder.value)
                        }
                        podcastDao.insert(response.podcastToDatabase())
                        latestPubDate = response.latestPubDate
                        earliestPubDate = response.earliestPubDate
                        topEpisodes = response.episodesToDatabase()
                    } catch (e: IOException) {
                        emit(State.Error(Exception(NO_CONNECTION)))
                        currentCoroutineContext().cancel()
                        return@flow
                    } catch (e: HttpException) {
                        emit(State.Error(Exception(NO_CONNECTION)))
                        currentCoroutineContext().cancel()
                        return@flow
                    }
                }
            } else {
                // If updateDate is null, there is no podcast in the database, fetch it.
                Timber.d("There is no podcast in the database, fetch it")
                try {
                    val response = withContext(ioDispatcher) {
                        listenApi.getPodcast(id, null, sortOrder.value)
                    }
                    podcastDao.insert(response.podcastToDatabase())
                    latestPubDate = response.latestPubDate
                    earliestPubDate = response.earliestPubDate
                    topEpisodes = response.episodesToDatabase()
                } catch (e: IOException) {
                    emit(State.Error(Exception(NO_CONNECTION)))
                    currentCoroutineContext().cancel()
                    return@flow
                } catch (e: HttpException) {
                    emit(State.Error(Exception(NO_CONNECTION)))
                    currentCoroutineContext().cancel()
                    return@flow
                }
            }
        }

        // Declare 2 vars for the top and bottom podcast episodes depending on the current
        // sort order and 2 vars for the top and bottom episodes loaded to the db depending
        // on the current sort order.
        val topPubDate: Long
        val bottomPubDate: Long
        val topLoadedPubDate: Long?
        val bottomLoadedPubDate: Long?
        // Obtain declared vars depending on the current sort order.
        if (sortOrder == SortOrder.RECENT_FIRST) {
            topPubDate = latestPubDate
            bottomPubDate = earliestPubDate
            topLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(id)
            bottomLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(id)
        } else {
            topPubDate = earliestPubDate
            bottomPubDate = latestPubDate
            topLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(id)
            bottomLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(id)
        }

        // If topLoadedPubDate is null, there are no episodes in the db for this podcast.
        if (topLoadedPubDate == null) {
            // Fetch episodes depending on the current sort order.
            Timber.d("There are no episodes, fetch all")
            emit(State.Loading)
            Timber.d("Insert ${topEpisodes.size} top fetched episodes")
            // Insert the episodes that were fetched earlier.
            episodeDao.insert(topEpisodes)
            episodesLoaded = topEpisodes.size
            // Fetch next episodes sequentially until the end.
            val nextEpisodePubDate = if (topEpisodes.isNotEmpty()) topEpisodes.last().date else null
            val result = fetchEpisodesSequentially(
                id,
                nextEpisodePubDate,
                bottomPubDate,
                sortOrder,
                true,
                episodesLoaded
            )
            // Stop fetching after reaching the limit.
            if (result is State.Success) {
                if (result.data >= EPISODES_LIMIT) {
                    Timber.d("$EPISODES_LIMIT or more episodes have been loaded, stop for now")
                    emit(State.Success(Unit))
                    currentCoroutineContext().cancel()
                    return@flow
                }
            }
            if (result is State.Error) {
                emit(result)
                currentCoroutineContext().cancel()
                return@flow
            }
        } else {
            // Check if there are episodes at the top that should be loaded.
            if (topLoadedPubDate == topPubDate) {
                // Top episodes are loaded already.
                Timber.d("Top episodes are loaded")

                // If there are episodes at the bottom to load, fetch them.
                if (bottomLoadedPubDate != bottomPubDate) {
                    Timber.d("Bottom episodes are not loaded, fetch them")
                    emit(State.Loading)
                    val result = fetchEpisodesSequentially(
                        id,
                        bottomLoadedPubDate!!,
                        bottomPubDate,
                        sortOrder,
                        true
                    )
                    // Stop fetching after reaching the limit.
                    if (result is State.Success) {
                        if (result.data >= EPISODES_LIMIT) {
                            Timber.d("$EPISODES_LIMIT or more episodes have been loaded, stop for now")
                            emit(State.Success(Unit))
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                    if (result is State.Error) {
                        emit(result)
                        currentCoroutineContext().cancel()
                        return@flow
                    }
                }
            } else {
                // There are episodes at top that should be loaded, fetch them in reverse order
                // to ensure that there are no spaces between loaded episodes in the database.
                // Fetch without the limit to ensure that the user will see the appropriate
                // episodes at the top of the list according to the sort order.
                Timber.d("Top episodes are not loaded, fetch them in reverse order")
                emit(State.Loading)
                var result = fetchEpisodesSequentially(
                    id,
                    topLoadedPubDate,
                    topPubDate,
                    sortOrder.reverse(),
                    false
                )
                if (result is State.Error) {
                    emit(result)
                    currentCoroutineContext().cancel()
                    return@flow
                }

                // If there are episodes at the bottom to load, fetch them.
                if (bottomLoadedPubDate != bottomPubDate) {
                    Timber.d("Bottom episodes are not loaded, fetch them")
                    result = fetchEpisodesSequentially(
                        id,
                        bottomLoadedPubDate!!,
                        bottomPubDate,
                        sortOrder,
                        true
                    )
                    // Stop fetching after reaching the limit.
                    if (result is State.Success) {
                        Timber.d("$EPISODES_LIMIT or more episodes have been loaded, stop for now")
                        if (result.data >= EPISODES_LIMIT) {
                            emit(State.Success(Unit))
                            currentCoroutineContext().cancel()
                            return@flow
                        }
                    }
                    if (result is State.Error) {
                        emit(result)
                        currentCoroutineContext().cancel()
                        return@flow
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
        startPubDate: Long?,
        endPubDate: Long,
        sortOrder: SortOrder,
        withLimit: Boolean,
        episodesLoaded: Int = 0
    ): State<Int> {
        var mEpisodesLoaded = episodesLoaded
        var nextEpisodePubDate = startPubDate
        return try {
            withContext(ioDispatcher) {
                while (nextEpisodePubDate != endPubDate) {
                    val response = listenApi.getPodcast(id, nextEpisodePubDate, sortOrder.value)
                    episodeDao.insert(response.episodesToDatabase())
                    nextEpisodePubDate = response.nextEpisodePubDate
                    Timber.d("$sortOrder: ${response.episodes.size} more episodes loaded with")
                    // Stop fetching after reaching the limit.
                    if (withLimit) {
                        mEpisodesLoaded += response.episodes.size
                        if (mEpisodesLoaded >= EPISODES_LIMIT) {
                            Timber.d("Break after exceeding the limit")
                            break
                        }
                    }
                }
            }
            State.Success(mEpisodesLoaded)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }
}