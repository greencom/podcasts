package com.greencom.android.podcasts.di

import com.greencom.android.podcasts.repository.PlayerRepository
import com.greencom.android.podcasts.repository.PlayerRepositoryImpl
import com.greencom.android.podcasts.repository.Repository
import com.greencom.android.podcasts.repository.RepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Hilt module that provides repository-related components. */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Tell Hilt which [Repository] implementation to use when it needs to provide
     * an instance of interface.
     */
    @Binds
    abstract fun bindRepository(repositoryImpl: RepositoryImpl): Repository

    // TODO
    @Binds
    abstract fun bindPlayerRepository(playerRepositoryImpl: PlayerRepositoryImpl): PlayerRepository
}