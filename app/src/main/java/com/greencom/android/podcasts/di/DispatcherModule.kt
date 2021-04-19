package com.greencom.android.podcasts.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/** Hilt module that provides coroutine dispatchers. */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /** Provides [Dispatchers.Main]. */
    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /** Provides [Dispatchers.IO]. */
    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** Provides [Dispatchers.Default]. */
    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** Annotation for a [Dispatchers.Main] dependency. */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class MainDispatcher

    /** Annotation for a [Dispatchers.IO] dependency. */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class IoDispatcher

    /** Annotation for a [Dispatchers.Default] dependency. */
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class DefaultDispatcher
}