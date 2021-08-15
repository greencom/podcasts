package com.greencom.android.podcasts.di

import android.content.Context
import androidx.room.Room
import com.greencom.android.podcasts.data.database.AppDatabase
import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.PodcastDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val APP_DATABASE_NAME = "podcasts_database"

/** Hilt module that provides database-related components. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Provides a singleton of the [AppDatabase]. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            APP_DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /** Provides access to the [PodcastDao]. */
    @Provides
    fun providePodcastDao(database: AppDatabase): PodcastDao {
        return database.podcastDao()
    }

    /** Provides access to the [EpisodeDao]. */
    @Provides
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao {
        return database.episodeDao()
    }
}