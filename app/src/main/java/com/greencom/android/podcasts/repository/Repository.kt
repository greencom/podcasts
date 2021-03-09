package com.greencom.android.podcasts.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.greencom.android.podcasts.data.asGenreEntities
import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.data.database.PodcastDao
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.IOException
import retrofit2.HttpException
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

    private val _genresState = MutableStateFlow<State>(State.NotLoading)
    /** Represents the state of loading genres using [State] class. */
    val genresState: StateFlow<State> = _genresState

    /** TODO: Documentation */
    fun getBestPodcasts(genreId: Int): Flow<PagingData<Podcast>> {
        return Pager(
            PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { BestPodcastsPagingSource(genreId, listenApi) }
        ).flow
    }

    /**
     * Fetch genre list from ListenAPI and insert it into the `genres` table,
     * if the table is empty. Use [genresState] to get the state of loading process.
     */
    suspend fun fetchGenres() {
        if (genreDao.getSize() == 0) {
            _genresState.value = State.Loading
            try {
                val genres = listenApi.getGenres().asGenreEntities()
                genreDao.insert(genres)
                _genresState.value = State.Success(Unit)
            } catch (e: IOException) {
                _genresState.value = State.Error(e)
            } catch (e: HttpException) {
                _genresState.value = State.Error(e)
            }
        } else {
            _genresState.value = State.Success(Unit)
        }
    }
}