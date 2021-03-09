package com.greencom.android.podcasts.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.data.updateSubscription
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class PodcastDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var podcastDao: PodcastDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        podcastDao = database.podcastDao()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun update_updateSubscription_returnUpdated() = runBlockingTest {
        // GIVEN - Insert a podcast.
        val podcast = PodcastEntity(
            id = "id",
            title = "title",
            description = "description",
            image = "image",
            publisher = "publisher",
            explicitContent = false,
            episodeCount = 100,
            latestPubDate = 1000000,
            inBestForGenre = Podcast.NOT_IN_BEST,
            inSubscriptions = false
        )
        podcastDao.insert(listOf(podcast))

        // WHEN - Change `inSubscriptions` value and update podcast.
        podcastDao.update(podcast.updateSubscription())
        val loaded = podcastDao.getPodcast(podcast.id)

        // THEN - The `inSubscription` value is back to the initial.
        assertThat(loaded?.title).isEqualTo(podcast.title)
        assertThat(loaded?.inSubscriptions).isEqualTo(!podcast.inSubscriptions)
    }
}