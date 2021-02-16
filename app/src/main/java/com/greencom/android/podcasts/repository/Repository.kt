package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.network.asDatabaseModel
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
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
     * Fetch genre list from ListenAPI and insert it into the `genres` table,
     * if the table is empty. Use [genresState] to get the state of loading process.
     */
    suspend fun loadGenres() = withContext(Dispatchers.IO) {
        if (genreDao.getSize() == 0) {
            _genresState.value = State.Loading
            try {
                val genres = listenApi.getGenres().asDatabaseModel()
                genreDao.insert(genres)
                _genresState.value = State.Success(Unit)
            } catch (e: Exception) {
                _genresState.value = State.Error(e)
            }
        } else {
            _genresState.value = State.Success(Unit)
        }
    }
}