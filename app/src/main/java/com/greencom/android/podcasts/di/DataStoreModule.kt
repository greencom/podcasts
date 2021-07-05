package com.greencom.android.podcasts.di

import com.greencom.android.podcasts.data.datastore.PreferenceStorage
import com.greencom.android.podcasts.data.datastore.PreferenceStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Hilt module that provides DataStore-related components. */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    /**
     * Tell Hilt which [PreferenceStorage] implementation to use when it needs to provide
     * an instance of the interface.
     */
    @Binds
    abstract fun bindPreferenceStorage(preferenceStorageImpl: PreferenceStorageImpl): PreferenceStorage
}