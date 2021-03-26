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
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    super.log(priority, "global___$tag", message, t)
                }

                override fun createStackElementTag(element: StackTraceElement): String {
                    return String.format(
                        "%s/%s",
                        super.createStackElementTag(element),
                        element.methodName,
                    )
                }
            })
        }
    }
}