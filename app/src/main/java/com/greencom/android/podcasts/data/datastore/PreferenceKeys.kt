package com.greencom.android.podcasts.data.datastore

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** DataStore preferences keys. */
object PreferenceKeys {

    /** Int preferences key to store and retrieve the app theme mode. */
    val THEME_MODE = intPreferencesKey("THEME_MODE")

    /** String preferences key to store and retrieve the ID of the last played episode. */
    val LAST_EPISODE_ID = stringPreferencesKey("LAST_EPISODE_ID")

    /** Float preferences key to store and retrieve a player playback speed. */
    val PLAYBACK_SPEED = floatPreferencesKey("PLAYBACK_SPEED")

    /** Int preferences key to store and retrieve subscription presentation mode. */
    val SUBSCRIPTION_MODE = intPreferencesKey("SUBSCRIPTIONS_MODE")
}