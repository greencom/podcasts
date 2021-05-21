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

    override suspend fun fetchPodcast(id: String, sortOrder: SortOrder): State<PodcastWrapper> {
        return try {
            val response = withContext(ioDispatcher) {
                listenApi.getPodcast(id, null, sortOrder.value)
            }
            podcastDao.insert(response.podcastToDatabase())
            State.Success(response)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }

    override fun getEpisodes(podcastId: String): Flow<List<Episode>> {
        return episodeDao.getEpisodesFlow(podcastId)
    }

    override suspend fun fetchEpisodes(
        id: String,
        sortOrder: SortOrder,
        isForced: Boolean
    ): State<Unit> {

        // Get episode count that has been loaded for this podcast.
        var episodesLoaded = episodeDao.getEpisodeCount(id)

        // Podcast latest and earliest pub dates.
        val latestPubDate: Long?
        val earliestPubDate: Long?
        var topEpisodes = listOf<EpisodeEntity>()

        // Whether the update was forced by the user.
        if (isForced) {
            // Update the podcast data regardless of the last update time.

            when (val result = fetchPodcast(id, sortOrder)) {
                is State.Success -> {
                    latestPubDate = result.data.latestPubDate
                    earliestPubDate = result.data.earliestPubDate
                    topEpisodes = result.data.episodesToDatabase()
                }
                is State.Error -> return result
                // Impossible case.
                else -> throw Exception()
            }
        } else {
            // Check when the podcast data was updated last time.
            val updateDate = podcastDao.getUpdateDate(id)

            if (updateDate != null) {
                // There is the podcast in the database. Calculate time since the last update.
                val timeFromLastUpdate = System.currentTimeMillis() - updateDate

                if (timeFromLastUpdate <= TimeUnit.HOURS.toMillis(3)) {
                    // If the podcast data was recently updated, get latestPubDate and
                    // earliestPubDate from the database.
                    latestPubDate = podcastDao.getLatestPubDate(id)!!
                    earliestPubDate = podcastDao.getEarliestPubDate(id)!!
                } else {
                    // If the podcast data was updated more than 3 hours ago, update the podcast
                    // data.

                    when (val result = fetchPodcast(id, sortOrder)) {
                        is State.Success -> {
                            latestPubDate = result.data.latestPubDate
                            earliestPubDate = result.data.earliestPubDate
                            topEpisodes = result.data.episodesToDatabase()
                        }
                        is State.Error -> return result
                        // Impossible case.
                        else -> throw Exception()
                    }
                }
            } else {
                // There is no podcast in the database, fetch it.

                when (val result = fetchPodcast(id, sortOrder)) {
                    is State.Success -> {
                        latestPubDate = result.data.latestPubDate
                        earliestPubDate = result.data.earliestPubDate
                        topEpisodes = result.data.episodesToDatabase()
                    }
                    is State.Error -> return result
                    // Impossible case.
                    else -> throw Exception()
                }
            }
        }

        // Top and bottom episodes pub dates for the podcast depending on the current sort order
        // and top and bottom episodes pub dates that were loaded in the database for this
        // podcast depending on the current sort order.
        val topPubDate: Long
        val bottomPubDate: Long
        val topLoadedPubDate: Long?
        val bottomLoadedPubDate: Long?

        // Obtain values.
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


        if (topLoadedPubDate == null) {
            // There are no episodes in the database for this podcast. Fetch episodes
            // depending on the current sort order.

            // Insert the episodes that were fetched earlier.
            episodeDao.insert(topEpisodes)
            episodesLoaded = topEpisodes.size

            // Fetch next episodes sequentially.
            val nextEpisodePubDate = if (topEpisodes.isNotEmpty()) topEpisodes.last().date else null
            val result = fetchEpisodesSequentially(
                id,
                nextEpisodePubDate,
                bottomPubDate,
                sortOrder,
                true,
                episodesLoaded
            )
            return when (result) {
                is State.Success -> State.Success(Unit)
                is State.Error -> result
                // Impossible case.
                else -> throw Exception()
            }
        } else {
            // There are episodes in the database for this podcast.

            // Check if there are episodes at the top that should be loaded.
            if (topLoadedPubDate == topPubDate) {
                // Top episodes are loaded already.

                // Return if the number of loaded episodes is greater than limit.
                if (episodesLoaded >= EPISODES_LIMIT) return State.Success(Unit)

                // Check if there are episodes at the bottom that should be loaded.
                if (bottomLoadedPubDate != bottomPubDate) {

                    val result = fetchEpisodesSequentially(
                        id,
                        bottomLoadedPubDate!!,
                        bottomPubDate,
                        sortOrder,
                        true,
                        episodesLoaded
                    )
                    return when (result) {
                        is State.Success -> State.Success(Unit)
                        is State.Error -> result
                        // Impossible case.
                        else -> throw Exception()
                    }
                }
            } else {
                // There are episodes at top that should be loaded, fetch them in reverse order
                // to ensure that there are no spaces between loaded episodes in the database.
                // Fetch without the limit to ensure that the user will see the appropriate
                // episodes at the top of the list according to the sort order.

                var result = fetchEpisodesSequentially(
                    id,
                    topLoadedPubDate,
                    topPubDate,
                    sortOrder.reverse(),
                    false
                )
                when (result) {
                    is State.Success -> episodesLoaded += result.data
                    is State.Error -> return result
                    // Impossible case.
                    else -> throw Exception()
                }

                // Return if the number of loaded episodes is greater than limit.
                if (episodesLoaded >= EPISODES_LIMIT) return State.Success(Unit)

                // Check if there are episodes at the bottom that should be loaded.
                if (bottomLoadedPubDate != bottomPubDate) {

                    result = fetchEpisodesSequentially(
                        id,
                        bottomLoadedPubDate!!,
                        bottomPubDate,
                        sortOrder,
                        true,
                        episodesLoaded
                    )
                    return when (result) {
                        is State.Success -> State.Success(Unit)
                        is State.Error -> result
                        // Impossible case.
                        else -> throw Exception()
                    }
                }
            }
        }

        return State.Success(Unit)
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

    /**
     * This method fetches podcast episodes sequentially from [startPubDate] to
     * [endPubDate] for [sortOrder]. Additionally, you can specify whether the method
     * needs to track the number of loaded episodes to stop when the number exceeds the
     * [EPISODES_LIMIT]. Returns result represented by [State] with the number of loaded
     * episodes.
     */
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
                    // Stop fetching after reaching the limit.
                    mEpisodesLoaded += response.episodes.size
                    if (withLimit) {
                        if (mEpisodesLoaded >= EPISODES_LIMIT) break
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