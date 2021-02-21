package com.greencom.android.podcasts.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.network.ListenApiService
import com.greencom.android.podcasts.network.asDomainModel
import okio.IOException
import retrofit2.HttpException

/** TODO: Documentation */
class BestPodcastsPagingSource(
    private val genreId: Int,
    private val listenApi: ListenApiService,
) : PagingSource<Int, Podcast>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Podcast> {
        return try {
            val page = params.key ?: 1
            val response = listenApi.getBestPodcasts(genreId, page)
            LoadResult.Page(
                data = response.asDomainModel(),
                prevKey = null,
                nextKey = null
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Podcast>): Int? {
        return null
    }
}