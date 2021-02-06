package com.greencom.android.podcasts.di

import com.greencom.android.podcasts.network.ListenApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** TODO: Documentation */
@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    /** TODO: Documentation */
    @Provides
    @Singleton
    fun provideListenApi(): ListenApi {
        return ListenApi
    }
}