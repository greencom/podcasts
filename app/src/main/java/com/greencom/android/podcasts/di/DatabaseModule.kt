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

/** TODO: Documentation */
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    /** TODO: Documentation */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "podcasts_database"
        ).build()
    }

    /** TODO: Documentation */
    @Provides
    fun provideGenreDao(database: AppDatabase): GenreDao {
        return database.genreDao()
    }
}