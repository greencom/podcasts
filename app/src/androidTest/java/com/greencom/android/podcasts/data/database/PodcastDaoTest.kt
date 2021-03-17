package com.greencom.android.podcasts.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.greencom.android.podcasts.createTestPodcastEntity
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
    fun getPodcast_podcastDoesNotExist_returnNull() = runBlockingTest {
        // GIVEN - Empty 'podcast_table'.

        // WHEN - Get a podcast.
        val loaded = podcastDao.getPodcast("id")

        // THEN - The podcast is 'null'.
        assertThat(loaded).isNull()
    }

    @Test
    fun getPodcast_podcastExists_returnPodcast() = runBlockingTest {
        // GIVEN - Insert the podcast into the 'podcast_table'.
        val podcast = createTestPodcastEntity("id")
        podcastDao.insert(podcast)
        val attrs = PodcastLocalAttrs(podcast.id, false)
        podcastDao.insertAttrs(attrs)

        // WHEN - Get a podcast.
        val loaded = podcastDao.getPodcast(podcast.id)

        // THEN - The podcast is valid.
        assertThat(loaded?.title).isEqualTo(podcast.title)
        assertThat(loaded?.subscribed).isEqualTo(attrs.subscribed)
    }

    @Test
    fun getBestPodcasts_bestPodcastsExist_returnPodcasts() = runBlockingTest {
        // GIVEN - Insert three podcasts into the 'podcast_table'.
        // Insert their local attributes into the 'podcast_local_table'.
        val podcastOne = createTestPodcastEntity("one")
        val podcastTwo = createTestPodcastEntity("two")
        val podcastThree = createTestPodcastEntity("three")
        val podcasts = listOf(podcastOne, podcastTwo, podcastThree)
        podcastDao.insert(podcasts)
        val attrsOne = PodcastLocalAttrs(podcastOne.id, true)
        val attrsTwo = PodcastLocalAttrs(podcastTwo.id, false)
        val attrsThree = PodcastLocalAttrs(podcastThree.id, true)
        podcastDao.insertAttrs(listOf(attrsOne, attrsTwo, attrsThree))

        // WHEN - Get the best podcasts for genreId = 1.
        val loaded = podcastDao.getBestPodcasts(podcastOne.genreId)

        // THEN - Podcasts was returned with their local attributes.
        assertThat(loaded.size).isEqualTo(podcasts.size)
        assertThat(loaded[0].id).isEqualTo(podcastOne.id)
        assertThat(loaded[0].subscribed).isEqualTo(attrsOne.subscribed)
    }

    @Test
    fun updateAttrs_updateSubscription_returnUpdatedPodcast() = runBlockingTest {
        // GIVEN - Insert a podcast without subscription.
        val podcast = createTestPodcastEntity("id")
        val podcastAttrs = PodcastLocalAttrs(podcast.id, false)
        podcastDao.insert(podcast)
        podcastDao.insertAttrs(podcastAttrs)

        // WHEN - Update `subscribed` property and get the podcast.
        val newAttrs = PodcastLocalAttrs(podcast.id, podcastAttrs.subscribed.not())
        podcastDao.updateAttrs(newAttrs)
        val loaded = podcastDao.getPodcast(podcast.id)

        // THEN - The podcast was updated.
        assertThat(loaded?.subscribed).isEqualTo(!podcastAttrs.subscribed)
    }
}