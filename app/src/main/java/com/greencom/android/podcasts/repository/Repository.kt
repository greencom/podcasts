package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.PodcastSearchResult
import com.greencom.android.podcasts.data.domain.PodcastShort
import com.greencom.android.podcasts.data.domain.PodcastWithEpisodes
import com.greencom.android.podcasts.network.PodcastWrapper
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel
import com.greencom.android.podcasts.utils.SortOrder
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

/**
 * App repository interface. Provides access to the ListenAPI network service
 * and the app database tables.
 */
interface Repository {

    /** Clear the whole database. */
    suspend fun deleteAll()

    /** Remove all episodes from the database. */
    suspend fun deleteEpisodes()

    /**
     * Fetch the podcast for a given ID from ListenAPI and insert it into the database.
     * Returns the result represented by [State].
     */
    suspend fun fetchPodcast(
        podcastId: String,
        sortOrder: SortOrder = SortOrder.RECENT_FIRST
    ): State<PodcastWrapper>

    /**
     * Fetch episodes for a given podcast ID for certain sort order and insert them into
     * the database. Returns the result represented by [State].
     *
     * This method first loads all episodes at the top of the list (depending on the current
     * sort order) with no limit and then loads episodes at the bottom of the list until the
     * number of all episodes loaded to the database for this podcast exceeds the limit.
     */
    suspend fun fetchEpisodes(
        podcastId: String,
        sortOrder: SortOrder,
        isForced: Boolean,
        event: Channel<PodcastViewModel.PodcastEvent>
    ): State<Unit>

    /**
     * Fetch more episodes on scroll for a given podcast ID for certain sort order and insert
     * them into the database. Returns the result represented by [State] with the number of
     * loaded episodes.
     */
    suspend fun fetchMoreEpisodes(
        podcastId: String,
        sortOrder: SortOrder,
        event: Channel<PodcastViewModel.PodcastEvent>
    ): State<Int>

    /**
     * Fetch the best podcasts for a given genre ID from ListenAPI and insert them
     * into the database. Returns result represented by [State].
     */
    suspend fun fetchBestPodcasts(genreId: Int): State<Unit>

    /**
     * Refresh the best podcasts for a given genre ID. A new list will be fetched from
     * ListenAPI and inserted into the database. Podcasts that not anymore on the best list
     * will be excluded from it, but remain in the database. Returns result represented by
     * [State].
     */
    suspend fun refreshBestPodcasts(genreId: Int, currentList: List<PodcastShort>): State<Unit>

    /**
     * Return a podcast with episodes for a given ID. The result represented by instances of
     * [State]. If the database already contains the appropriate podcast, return it. Otherwise,
     * fetch the podcast from ListenAPI and insert it into the database.
     */
    fun getPodcastWithEpisodes(podcastId: String): Flow<State<PodcastWithEpisodes>>

    /**
     * Return an episode for a given ID from the database. The result represented by
     * instances of [State]. If there is no such episode in the database, emits [State.Error].
     */
    fun getEpisode(episodeId: String): Flow<State<Episode>>

    /** Get a Flow with a list of subscriptions represented by [PodcastShort]. */
    fun getSubscriptions(): Flow<List<PodcastShort>>

    /** Set app theme mode. */
    suspend fun setTheme(mode: Int)

    /** Get a Flow with the app theme mode. */
    fun getTheme(): Flow<Int?>

    /** Save subscription presentation mode. */
    suspend fun setSubscriptionMode(mode: Int)

    /** Get a Flow with a subscription presentation mode. */
    fun getSubscriptionMode(): Flow<Int?>

    /** Get a Flow with a player playback speed. */
    fun getPlaybackSpeed(): Flow<Float?>

    /**
     * Return the best podcasts for a given genre ID. The result presented by instances of
     * [State]. If the database already contains the appropriate podcasts, return them.
     * Otherwise, fetch the podcasts from ListenAPI and insert them into the database.
     */
    fun getBestPodcasts(genreId: Int): Flow<State<List<PodcastShort>>>

    /** Get a Flow with a list of completed [Episode]s in the descending order of the end date. */
    fun getEpisodeHistory(): Flow<List<Episode>>

    /**
     * Get a Flow with a list of episodes that have been added to the bookmarks in the
     * descending order of the add date.
     */
    fun getBookmarks(): Flow<List<Episode>>

    /**
     * Get a Flow with a list of episodes in progress in descending order of the last
     * played date.
     */
    fun getEpisodesInProgress(): Flow<List<Episode>>

    /** Search for a podcast by given arguments, cache the result and return it as [State]. */
    suspend fun searchPodcast(query: String, offset: Int): State<PodcastSearchResult>

    /** Returns the last search that was cached in the Repository. */
    fun getLastSearch(): PodcastSearchResult?

    /** Update subscription to a Podcast by ID with a given value. */
    suspend fun onPodcastSubscribedChange(podcastId: String, subscribed: Boolean)

    /** Add the episode to the bookmarks or remove from there. */
    suspend fun onEpisodeInBookmarksChange(episodeId: String, inBookmarks: Boolean)
}