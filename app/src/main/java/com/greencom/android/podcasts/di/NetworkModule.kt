package com.greencom.android.podcasts.di

import com.greencom.android.podcasts.network.ListenApi
import com.greencom.android.podcasts.network.ListenApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that provides network-related components. */
@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    /**
     * Provides a singleton of the [ListenApi.service] instance that implements
     * [ListenApiService] interface.
     */
    @Provides
    @Singleton
    fun provideListenApiService(): ListenApiService {
        return ListenApi.service
    }
}