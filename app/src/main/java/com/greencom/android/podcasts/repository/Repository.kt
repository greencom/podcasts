package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.network.ListenApi
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
    private val listenApi: ListenApi
) {

    /**
     * Fetch genres from ListenAPI and insert them into the `genres` table,
     * if the table is empty.
     */
    suspend fun updateGenres() = withContext(Dispatchers.IO) {
        Timber.d("updateGenres() called. Size of the genres table is ${genreDao.getSize()}")
        if (genreDao.getSize() == 0) {
            Timber.d("`If` statement passed")
            try {
                val genres = listenApi.service.getGenres().asDatabaseModel()
                Timber.d("Genres are fetched from ListenAPI")
                genreDao.insertAll(genres)
                Timber.d("Genres are inserted into the database")
            } catch (e: Exception) {
                Timber.d("Error caught: ${e.message}")
            }
        }
    }
}