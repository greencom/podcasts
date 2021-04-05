package com.greencom.android.podcasts.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class PodcastDaoTest {

//    @get:Rule
//    var instantExecutorRule = InstantTaskExecutorRule()

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
    @Throws(IOException::class)
    fun closeDatabase() = database.close()

    @Test
    @Throws(Exception::class)
    fun getPodcast_emptyTable_returnNull() {
        // GIVEN: Empty `podcasts` table.

        // WHEN: Get a podcast.
        val loaded = podcastDao.getPodcast("id")

        // THEN: loaded is null.
        assertThat(loaded).isNull()
    }

//    @Test
//    @Throws(Exception::class)
//    fun getPodcast_podcastExists_returnPodcast() {
//        TODO()
//    }

    @Test
    @Throws(Exception::class)
    fun insertPartial_returnPodcast() {
        // GIVEN: Insert a partial podcast into the `podcasts_temp` table.
        val podcast = PodcastEntityPartial(
            "id",
            "title",
            "desc",
            "image",
            "publisher",
            false,
            100,
            100L
        )
        podcastDao.insertPartial(podcast)

        // WHEN: Get a podcast by ID.
        val loaded = podcastDao.getPodcastFromTemp(podcast.id)

        // THEN: The podcast is not null, `genreId` = -1, `subscribed` = false.
        assertThat(loaded?.title).isEqualTo(podcast.title)
        assertThat(loaded?.genreId).isEqualTo(-1)
        assertThat(loaded?.subscribed).isFalse()
    }
}