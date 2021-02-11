package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.network.asDatabaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** App repository. */
@Singleton
class Repository @Inject constructor(
    private val genreDao: GenreDao,
//    private val listenApi: ListenApi,
    private val listenApi: ListenApiService,
) {

    /**
     * Fetch genres from ListenAPI and insert them into the `genres` table,
     * if the table is empty.
     */
    suspend fun updateGenres() = withContext(Dispatchers.IO) {
        if (genreDao.getSize() == 0) {
            try {
                val genres = listenApi.getGenres().asDatabaseModel()
                genreDao.insertAll(genres)
                Timber.d("Genres updated")
            } catch (e: Exception) {
                Timber.d("Error caught: ${e.message}")
            }
        }
    }
}