package com.greencom.android.podcasts.di

import com.greencom.android.podcasts.network.ListenApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/** Hilt module that provides network-related components. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** ListenAPI base URL. */
    private const val BASE_URL = "https://listen-api.listennotes.com/api/v2/"

    /** Provides the implementation of [ListenApiService]. */
    @Singleton
    @Provides
    fun provideListenApiService(moshi: Moshi, httpClient: OkHttpClient): ListenApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(httpClient)
            .build()
            .create(ListenApiService::class.java)
    }

    /** Provides an instance of [Moshi] for [MoshiConverterFactory]. */
    @Singleton
    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    /** Provides an instance of [OkHttpClient] with logging to use in `Retrofit.Builder()`. */
    @Singleton
    @Provides
    fun provideHttpClient(logging: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    /** Provides [HttpLoggingInterceptor] of [HttpLoggingInterceptor.Level.BASIC] level. */
    @Singleton
    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
    }
}