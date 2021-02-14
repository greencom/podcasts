package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.network.asDatabaseModel
import com.greencom.android.podcasts.utils.GenresState
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
    private val genreDao: GenreDao,
    private val listenApi: ListenApiService,
) {

    // Backing property.
    private val _genresState = MutableStateFlow<GenresState>(GenresState.Init)
    /** Represents the state of loading genres using [GenresState] class. */
    val genresState: StateFlow<GenresState> = _genresState

    /**
     * Fetch genre list from ListenAPI and insert it into the `genres` table,
     * if the table is empty.
     */
    suspend fun loadGenres() = withContext(Dispatchers.IO) {
        if (genreDao.getSize() == 0) {
            _genresState.value = GenresState.Loading
            try {
                val genres = listenApi.getGenres().asDatabaseModel()
                genreDao.insertAll(genres)
                _genresState.value = GenresState.Success
            } catch (e: Exception) {
                _genresState.value = GenresState.Error(e)
            }
        } else {
            _genresState.value = GenresState.Success
        }
    }
}