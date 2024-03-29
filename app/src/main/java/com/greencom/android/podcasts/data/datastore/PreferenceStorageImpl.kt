package com.greencom.android.podcasts.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val DATA_STORE_NAME = "preference_storage"

private val Context.dataStore by preferencesDataStore(DATA_STORE_NAME)

/**
 * Implements [PreferenceStorage] interface and provides access to the storage that uses
 * DataStore APIs.
 */
@Singleton
class PreferenceStorageImpl @Inject constructor(
    @ApplicationContext appContext: Context
) : PreferenceStorage {

    private val dataStore = appContext.dataStore

    override suspend fun setTheme(mode: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = mode
        }
    }

    override fun getTheme(): Flow<Int?> {
        return dataStore.data
            .catch { handleException(it) }
            .map { preferences ->
                preferences[PreferenceKeys.THEME_MODE]
            }
    }

    override suspend fun setPlaybackSpeed(playbackSpeed: Float) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.PLAYBACK_SPEED] = playbackSpeed
        }
    }

    override fun getPlaybackSpeed(): Flow<Float?> {
        return dataStore.data
            .catch { handleException(it) }
            .map { preferences ->
                preferences[PreferenceKeys.PLAYBACK_SPEED]
            }
    }

    override suspend fun setLastEpisodeId(episodeId: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_EPISODE_ID] = episodeId
        }
    }

    override fun getLastEpisodeId(): Flow<String?> {
        return dataStore.data
            .catch { handleException(it) }
            .map { preferences ->
                preferences[PreferenceKeys.LAST_EPISODE_ID]
            }
    }

    override suspend fun setSubscriptionMode(mode: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SUBSCRIPTION_MODE] = mode
        }
    }

    override fun getSubscriptionMode(): Flow<Int?> {
        return dataStore.data
            .catch { handleException(it) }
            .map { preferences ->
                preferences[PreferenceKeys.SUBSCRIPTION_MODE]
            }
    }

    private suspend fun FlowCollector<Preferences>.handleException(exception: Throwable) {
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
}