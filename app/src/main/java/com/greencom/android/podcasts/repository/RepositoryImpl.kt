package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.*
import com.greencom.android.podcasts.data.datastore.PreferenceStorage
import com.greencom.android.podcasts.data.domain.*
import com.greencom.android.podcasts.di.DispatcherModule.IoDispatcher
import com.greencom.android.podcasts.network.*
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel
import com.greencom.android.podcasts.utils.ImpossibleCaseException
import com.greencom.android.podcasts.utils.SortOrder
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
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
private const val EPISODES_LIMIT_DEFAULT = 30
private const val EPISODES_LIMIT_NONE = 0

private const val PODCAST_MAX_HOURS_FROM_UPDATE = 6L

/**
 * App [Repository] implementation. Provides access to the ListenAPI network service
 * and the app database tables.
 */
@Singleton
class RepositoryImpl @Inject constructor(
    private val preferenceStorage: PreferenceStorage,
    private val listenApi: ListenApiService,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : Repository {

    // TODO: Test code.
    override suspend fun deleteAll() {
        podcastDao.clear()
        episodeDao.clear()
    }

    // TODO: Test code.
    override suspend fun deleteEpisodes() = episodeDao.clear()

    /**
     * Is retrofit main-safe to call from Dispatchers.Main or not. Used inside [safeRetrofitCall]
     * to do the first call in [withContext] wrapper and any other without it. See more in
     * [safeRetrofitCall] documentation.
     */
    private var isRetrofitSafe = false

    /** Cached last search result. */
    private var searchResult: PodcastSearchResult? = null

    override suspend fun fetchPodcast(podcastId: String, sortOrder: SortOrder): State<PodcastWrapper> {
        return try {
            val response = safeRetrofitCall {
                listenApi.getPodcast(podcastId, null, sortOrder.value)
            }
            podcastDao.insert(response.podcastToDatabase())
            State.Success(response)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }

    override suspend fun fetchEpisodes(
        podcastId: String,
        sortOrder: SortOrder,
        isForced: Boolean,
        event: Channel<PodcastViewModel.PodcastEvent>
    ): State<Unit> {
        // Get episode count that has been loaded for this podcast.
        var episodesLoaded = episodeDao.getEpisodeCount(podcastId)

        // Podcast latest and earliest pub dates.
        val latestPubDate: Long?
        val earliestPubDate: Long?
        var topEpisodes = listOf<EpisodeEntity>()

        // Check if update was forced by the user.
        if (isForced) {
            // isForced == true
            // Update the podcast data regardless of the last update time.
            startEpisodesLoadingIndicator(isForced, event)
            when (val result = fetchPodcast(podcastId, sortOrder)) {
                is State.Success -> {
                    latestPubDate = result.data.latestPubDate
                    earliestPubDate = result.data.earliestPubDate
                    topEpisodes = result.data.episodesToDatabase()
                }
                is State.Error -> return result
                else -> throw ImpossibleCaseException()
            }
        } else {
            // isForced == false
            // Check when the podcast data was updated last time.
            val updateDate = podcastDao.getUpdateDate(podcastId)

            if (updateDate != null) {
                // updateDate != null
                // There is the podcast in the database. Calculate time since the last update.
                val timeFromLastUpdate = System.currentTimeMillis() - updateDate

                if (timeFromLastUpdate <= TimeUnit.HOURS.toMillis(PODCAST_MAX_HOURS_FROM_UPDATE)) {
                    // If the podcast data was recently updated, get latestPubDate and
                    // earliestPubDate from the database.
                    // !! operators are safe here since we are sure there is the podcast in the db.
                    latestPubDate = podcastDao.getLatestPubDate(podcastId)!!
                    earliestPubDate = podcastDao.getEarliestPubDate(podcastId)!!
                } else {
                    // If the podcast data was updated more than 3 hours ago, update the podcast.
                    startEpisodesLoadingIndicator(isForced, event)
                    when (val result = fetchPodcast(podcastId, sortOrder)) {
                        is State.Success -> {
                            latestPubDate = result.data.latestPubDate
                            earliestPubDate = result.data.earliestPubDate
                            topEpisodes = result.data.episodesToDatabase()
                        }
                        is State.Error -> return result
                        else -> throw ImpossibleCaseException()
                    }
                }
            } else {
                // updateDate == null
                // There is no podcast in the database, fetch it.
                startEpisodesLoadingIndicator(isForced, event)
                when (val result = fetchPodcast(podcastId, sortOrder)) {
                    is State.Success -> {
                        latestPubDate = result.data.latestPubDate
                        earliestPubDate = result.data.earliestPubDate
                        topEpisodes = result.data.episodesToDatabase()
                    }
                    is State.Error -> return result
                    else -> throw ImpossibleCaseException()
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
        when (sortOrder) {
            SortOrder.RECENT_FIRST -> {
                topPubDate = latestPubDate
                bottomPubDate = earliestPubDate
                topLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(podcastId)
                bottomLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(podcastId)
            }
            SortOrder.OLDEST_FIRST -> {
                topPubDate = earliestPubDate
                bottomPubDate = latestPubDate
                topLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(podcastId)
                bottomLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(podcastId)
            }
        }


        if (topLoadedPubDate == null) {
            // topLoadedPubDate == null
            // There are no episodes in the database for this podcast. Fetch episodes
            // depending on the current sort order.

            // Insert the episodes that were fetched earlier.
            startEpisodesLoadingIndicator(isForced, event)
            episodeDao.insert(topEpisodes)
            episodesLoaded = topEpisodes.size

            // Fetch next episodes sequentially.
            val nextEpisodePubDate = if (topEpisodes.isNotEmpty()) {
                topEpisodes.last().date
            } else {
                null
            }
            val result = fetchEpisodesSequentially(
                podcastId = podcastId,
                startPubDate = nextEpisodePubDate,
                endPubDate = bottomPubDate,
                sortOrder = sortOrder,
                limit = EPISODES_LIMIT_DEFAULT,
                episodesLoaded = episodesLoaded
            )
            return when (result) {
                is State.Success -> State.Success(Unit)
                is State.Error -> result
                else -> throw ImpossibleCaseException()
            }
        } else {
            // topLoadedPubDate != null
            // There are episodes in the database for this podcast.

            // Check if there are episodes at the top that should be loaded.
            if (topLoadedPubDate == topPubDate) {
                // topLoadedPubDate == topPubDate
                // Top episodes are loaded already.

                // Return if the number of loaded episodes is greater than limit.
                if (episodesLoaded >= EPISODES_LIMIT_DEFAULT) {
                    return State.Success(Unit)
                }

                // Check if there are episodes at the bottom that should be loaded.
                if (bottomLoadedPubDate != bottomPubDate) {
                    startEpisodesLoadingIndicator(isForced, event)
                    val result = fetchEpisodesSequentially(
                        podcastId = podcastId,
                        startPubDate = bottomLoadedPubDate!!,
                        endPubDate = bottomPubDate,
                        sortOrder = sortOrder,
                        limit = EPISODES_LIMIT_DEFAULT,
                        episodesLoaded = episodesLoaded
                    )
                    return when (result) {
                        is State.Success -> State.Success(Unit)
                        is State.Error -> result
                        else -> throw ImpossibleCaseException()
                    }
                }
            } else {
                // topLoadedPubDate != topPubDate
                // There are episodes at the top that should be loaded, fetch them in reverse order
                // to ensure that there are no spaces between loaded episodes in the database.
                // Fetch without the limit to ensure that the user will see the appropriate
                // episodes at the top of the list according to the sort order.
                startEpisodesLoadingIndicator(isForced, event)
                var result = fetchEpisodesSequentially(
                    podcastId = podcastId,
                    startPubDate = topLoadedPubDate,
                    endPubDate = topPubDate,
                    sortOrder = sortOrder.reverse(),
                    limit = EPISODES_LIMIT_NONE // No limit!
                )
                when (result) {
                    is State.Success -> episodesLoaded += result.data
                    is State.Error -> return result
                    else -> throw ImpossibleCaseException()
                }

                // Return if the number of loaded episodes is greater than limit.
                if (episodesLoaded >= EPISODES_LIMIT_DEFAULT) {
                    return State.Success(Unit)
                }

                // Check if there are episodes at the bottom that should be loaded.
                if (bottomLoadedPubDate != bottomPubDate) {
                    startEpisodesLoadingIndicator(isForced, event)
                    result = fetchEpisodesSequentially(
                        podcastId = podcastId,
                        startPubDate = bottomLoadedPubDate!!,
                        endPubDate = bottomPubDate,
                        sortOrder = sortOrder,
                        limit = EPISODES_LIMIT_DEFAULT,
                        episodesLoaded = episodesLoaded
                    )
                    return when (result) {
                        is State.Success -> State.Success(Unit)
                        is State.Error -> result
                        else -> throw ImpossibleCaseException()
                    }
                }
            }
        }

        return State.Success(Unit)
    }

    override suspend fun fetchMoreEpisodes(
        podcastId: String,
        sortOrder: SortOrder,
        event: Channel<PodcastViewModel.PodcastEvent>
    ): State<Int> {
        val bottomPubDate: Long // Date of the earliest/latest podcast pub date.
        val bottomLoadedPubDate: Long // Date of the earliest/latest loaded episode pub date.

        // !! operators are safe here since the function can be called only if the episode
        // count is greater than 10 -> episodes exist.
        when (sortOrder) {
            SortOrder.RECENT_FIRST -> {
                bottomPubDate = podcastDao.getEarliestPubDate(podcastId)!!
                bottomLoadedPubDate = episodeDao.getEarliestLoadedEpisodePubDate(podcastId)!!
            }
            SortOrder.OLDEST_FIRST -> {
                bottomPubDate = podcastDao.getLatestPubDate(podcastId)!!
                bottomLoadedPubDate = episodeDao.getLatestLoadedEpisodePubDate(podcastId)!!
            }
        }

        // Return if all episodes have been loaded already.
        if (bottomLoadedPubDate == bottomPubDate) {
            return State.Success(0)
        }

        event.send(PodcastViewModel.PodcastEvent.EpisodesFetchingStarted)
        val result = fetchEpisodesSequentially(
            podcastId = podcastId,
            startPubDate = bottomLoadedPubDate,
            endPubDate = bottomPubDate,
            sortOrder = sortOrder,
            limit = EPISODES_LIMIT_DEFAULT
        )
        return when (result) {
            is State.Success -> result
            is State.Error -> result
            else -> throw ImpossibleCaseException()
        }
    }

    override suspend fun fetchBestPodcasts(genreId: Int): State<Unit> {
        return try {
            val response = safeRetrofitCall { listenApi.getBestPodcasts(genreId) }
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
            val newList = safeRetrofitCall {
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

    override fun getPodcastWithEpisodes(podcastId: String): Flow<State<PodcastWithEpisodes>> = flow {
        emit(State.Loading)
        podcastDao.getPodcastWithEpisodesFlow(podcastId).collect { podcastWithEpisodes ->
            // If the database already contains the appropriate podcast, return it.
            if (podcastWithEpisodes != null) {
                emit(State.Success(podcastWithEpisodes))
            } else {
                // Otherwise, fetch the podcast from ListenAPI and insert into the database.
                val result = fetchPodcast(podcastId)
                if (result is State.Error) emit(result)
            }
        }
    }

    override fun getEpisode(episodeId: String): Flow<State<Episode>> = flow {
        emit(State.Loading)
        episodeDao.getEpisodeFlow(episodeId).collect { episode ->
            if (episode != null) {
                emit(State.Success(episode))
            } else {
                emit(State.Error(Exception()))
            }
        }
    }

    override fun getSubscriptions(): Flow<List<PodcastShort>> {
        return podcastDao.getSubscriptionsFlow()
    }

    override suspend fun setSubscriptionMode(mode: Int) {
        preferenceStorage.setSubscriptionMode(mode)
    }

    override fun getSubscriptionMode(): Flow<Int?> {
        return preferenceStorage.getSubscriptionMode()
    }

    override fun getPlaybackSpeed(): Flow<Float?> {
        return preferenceStorage.getPlaybackSpeed()
    }

    override fun getBestPodcasts(genreId: Int): Flow<State<List<PodcastShort>>> = flow {
        emit(State.Loading)
        podcastDao.getBestPodcastsFlow(genreId).collect { podcasts ->
            // Return from the database, if it contains the appropriate podcasts.
            if (podcasts.isNotEmpty()) {
                emit(State.Success(podcasts))
            } else {
                // Otherwise, fetch the best podcasts from ListenAPI and insert them into the db.
                val result = fetchBestPodcasts(genreId)
                if (result is State.Error) emit(result)
            }
        }
    }

    override fun getEpisodeHistory(): Flow<List<Episode>> = episodeDao.getEpisodeHistoryFlow()

    override fun getBookmarks(): Flow<List<Episode>> = episodeDao.getBookmarksFlow()

    override fun getEpisodesInProgress(): Flow<List<Episode>> {
        return episodeDao.getEpisodesInProgressFlow()
    }

    override suspend fun searchPodcast(query: String, offset: Int): State<PodcastSearchResult> {
        // If search arguments are the same as they were for the previous one, return last result.
        searchResult?.let { searchResult ->
            if (query == searchResult.query && offset == searchResult.offset) {
                return State.Success(searchResult)
            }
        }

        return try {
            val response = safeRetrofitCall {
                listenApi.searchPodcast(query = query, offset = offset)
            }
            podcastDao.insert(response.podcastsToDatabase()) // Insert podcasts to the database.
            val result = response.toDomain(query, offset)
            // If the current search is a continuation of the previous one, combine lists.
            val list = searchResult?.let { searchResult ->
                if (query == searchResult.query) {
                    searchResult.podcasts + result.podcasts
                } else {
                    result.podcasts
                }
            } ?: result.podcasts
            val mSearchResult = result.copy(podcasts = list)
            searchResult = mSearchResult // Cache the result.
            State.Success(mSearchResult)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }

    override fun getLastSearch(): PodcastSearchResult? = searchResult

    override suspend fun updateSubscription(podcastId: String, subscribed: Boolean) {
        podcastDao.updateSubscription(PodcastSubscription(podcastId, subscribed))
    }

    override suspend fun updateEpisodeInBookmarks(episodeId: String, inBookmarks: Boolean) {
        val date = if (inBookmarks) System.currentTimeMillis() else 0L
        episodeDao.update(
            EpisodeEntityBookmark(
                id = episodeId,
                inBookmarks = inBookmarks,
                addedToBookmarksDate = date
            )
        )
    }

    override suspend fun markEpisodeCompletedOrUncompleted(
        episodeId: String,
        isCompleted: Boolean
    ) {
        val episodeState = if (isCompleted) {
            EpisodeEntityState(
                id = episodeId,
                position = 0L,
                lastPlayedDate = System.currentTimeMillis(),
                isCompleted = true,
                completionDate = System.currentTimeMillis()
            )
        } else {
            EpisodeEntityState(
                id = episodeId,
                position = 0L,
                lastPlayedDate = System.currentTimeMillis(),
                isCompleted = false,
                completionDate = 0L
            )
        }
        episodeDao.update(episodeState)

        // Remove the episode from the bookmarks if it is completed.
        if (isCompleted) {
            episodeDao.update(
                EpisodeEntityBookmark(
                    id = episodeId,
                    inBookmarks = false,
                    addedToBookmarksDate = episodeState.completionDate
                )
            )
        }
    }

    /**
     * Make a safe Retrofit call and return the result. The first overall call will be wrapped
     * in [withContext] function and any other will be done without it.
     *
     * Note: this function is needed due to some strange Retrofit bug, which produces UI freeze
     * on the first Retrofit call.
     */
    private suspend fun <T> safeRetrofitCall(block: suspend () -> T): T {
        return if (isRetrofitSafe) {
            block()
        } else {
            val result = withContext(ioDispatcher) { block() }
            isRetrofitSafe = true
            result
        }
    }

    /**
     * This method fetches podcast episodes sequentially from [startPubDate] to
     * [endPubDate] for [sortOrder]. Additionally, you can specify the [limit] and the start
     * count of the loaded episodes to stop when the number exceeds the limit. If the limit
     * is zero or [EPISODES_LIMIT_NONE], load without limit.
     * Returns result represented by [State] with the number of loaded episodes.
     */
    private suspend fun fetchEpisodesSequentially(
        podcastId: String,
        startPubDate: Long?,
        endPubDate: Long,
        sortOrder: SortOrder,
        limit: Int,
        episodesLoaded: Int = 0
    ): State<Int> {
        var mEpisodesLoaded = episodesLoaded
        var nextEpisodePubDate = startPubDate
        val withLimit = limit > 0

        return try {
            while (nextEpisodePubDate != endPubDate) {
                val response = safeRetrofitCall {
                    listenApi.getPodcast(podcastId, nextEpisodePubDate, sortOrder.value)
                }
                episodeDao.insert(response.episodesToDatabase())
                nextEpisodePubDate = response.nextEpisodePubDate
                mEpisodesLoaded += response.episodes.size
                // Stop fetching after reaching the limit.
                if (withLimit && mEpisodesLoaded >= limit) break
            }
            State.Success(mEpisodesLoaded)
        } catch (e: IOException) {
            State.Error(e)
        } catch (e: HttpException) {
            State.Error(e)
        }
    }

    /**
     * Used in [fetchEpisodes]. If the fetching was forced by the user, do nothing,
     * since the swipe-to-refreshed loading indicator has already started by the gesture.
     * Otherwise, push `EpisodesFetchingStarted` event to [event] channel.
     */
    private suspend fun startEpisodesLoadingIndicator(
        isForced: Boolean,
        event: Channel<PodcastViewModel.PodcastEvent>
    ) {
        if (!isForced) {
            event.send(PodcastViewModel.PodcastEvent.EpisodesFetchingStarted)
        }
    }
}