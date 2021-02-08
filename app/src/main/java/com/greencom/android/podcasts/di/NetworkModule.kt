package com.greencom.android.podcasts.di

import com.greencom.android.podcasts.network.ListenApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that provides network-related components. */
@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    /** Provides a singleton of the [ListenApi]. */
    @Provides
    @Singleton
    fun provideListenApi(): ListenApi {
        return ListenApi
    }
}