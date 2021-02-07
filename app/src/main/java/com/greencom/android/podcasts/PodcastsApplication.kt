package com.greencom.android.podcasts

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class PodcastsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Timber initialization.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
