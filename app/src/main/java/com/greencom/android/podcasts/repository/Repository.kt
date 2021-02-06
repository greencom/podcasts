package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.network.ListenApi
import javax.inject.Inject
import javax.inject.Singleton

/** TODO: Documentation */
@Singleton
class Repository @Inject constructor(
    private val genreDao: GenreDao,
    private val listenApi: ListenApi
) {


}