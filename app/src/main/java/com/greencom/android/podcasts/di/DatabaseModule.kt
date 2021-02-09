package com.greencom.android.podcasts.di

import android.content.Context
import androidx.room.Room
import com.greencom.android.podcasts.data.database.AppDatabase
import com.greencom.android.podcasts.data.database.GenreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that provides database-related components. */
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    /** Provides a singleton of the [AppDatabase]. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "podcasts_database"
        ).build()
    }

    /** Provides an instance of the [GenreDao]. */
    @Provides
    fun provideGenreDao(database: AppDatabase): GenreDao {
        return database.genreDao()
    }
}