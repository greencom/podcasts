package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.network.ListenApiService
import javax.inject.Inject
import javax.inject.Singleton

/** App repository. */
@Singleton
class Repository @Inject constructor(
    private val genreDao: GenreDao,
    private val listenApi: ListenApiService,
) {


}