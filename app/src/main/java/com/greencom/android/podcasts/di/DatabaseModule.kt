package com.greencom.android.podcasts.di

import android.content.Context
import androidx.room.Room
import com.greencom.android.podcasts.data.database.AppDatabase
import com.greencom.android.podcasts.data.database.EpisodeDao
import com.greencom.android.podcasts.data.database.GenreDao
import com.greencom.android.podcasts.data.database.PodcastDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that provides database-related components. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Provides a singleton of the [AppDatabase]. */
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "podcasts_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /** Provides an instance of the [PodcastDao]. */
    @Singleton
    @Provides
    fun providePodcastDao(database: AppDatabase): PodcastDao {
        return database.podcastDao()
    }

    /** Provides an instance of the [EpisodeDao]. */
    @Singleton
    @Provides
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao {
        return database.episodeDao()
    }

    /** Provides an instance of the [GenreDao]. */
    @Singleton
    @Provides
    fun provideGenreDao(database: AppDatabase): GenreDao {
        return database.genreDao()
    }
}