package com.greencom.android.podcasts.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
@SmallTest
class PodcastDaoTransactionsTest {

    private lateinit var database: AppDatabase
    private lateinit var podcastDao: PodcastDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).setTransactionExecutor(Executors.newSingleThreadExecutor()).build()
        podcastDao = database.podcastDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() = database.close()

    @Test
    @Throws(Exception::class)
    fun insertPartial_insertSinglePodcast_returnPodcast() = runBlocking {
        // GIVEN: `podcasts` table does not contain the podcast.

        // WHEN: Insert the podcast into the `podcasts` table.
        val podcast = PodcastEntityPartial(
            "one",
            "one",
            "one",
            "one",
            "one",
            false,
            100,
            100L
        )
        podcastDao.insertPartial(podcast)

        // THEN: loaded is valid.
        val loaded = podcastDao.getPodcast(podcast.id)
        assertThat(loaded?.title).isEqualTo(podcast.title)
        assertThat(loaded?.subscribed).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun insertPartial_podcastsTableContainsPartOfList_insertMissingAndUpdateExisting() =
        runBlocking {
            // GIVEN: `podcasts` table contains the podcast.
            val podcastOne = PodcastEntityPartial(
                "one",
                "one",
                "one",
                "one",
                "one",
                false,
                100,
                100L
            )
            podcastDao.insertPartial(podcastOne)

            // WHEN: Insert the podcasts into the `podcasts` table.
            val newOne = podcastOne.copy(title = "updated")
            val newTwo = podcastOne.copy("two", "two")
            val newThree = podcastOne.copy("three", "three")
            podcastDao.insertPartial(listOf(newOne, newTwo, newThree))

            // THEN: All podcasts are valid.
            val loadedOne = podcastDao.getPodcast(podcastOne.id)
            val loadedTwo = podcastDao.getPodcast(newTwo.id)
            val loadedThree = podcastDao.getPodcast(newThree.id)
            assertThat(loadedOne?.title).isEqualTo(newOne.title)
            assertThat(loadedTwo?.title).isEqualTo(newTwo.title)
            assertThat(loadedThree?.title).isEqualTo(newThree.title)
        }

    @Test
    @Throws(Exception::class)
    fun insertPartialWithGenre_insertSinglePodcast_returnPodcast() = runBlocking {
        // GIVEN: `podcasts` table does not contain the podcast.

        // WHEN: Insert the podcast into the `podcasts` table.
        val podcast = PodcastEntityPartialWithGenre(
            "one",
            "one",
            "one",
            "one",
            "one",
            false,
            100,
            100L,
            15
        )
        podcastDao.insertPartialWithGenre(podcast)

        // THEN: loaded is valid.
        val loaded = podcastDao.getPodcast(podcast.id)
        assertThat(loaded?.title).isEqualTo(podcast.title)
        assertThat(loaded?.subscribed).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun insertPartialWithGenre_podcastsTableContainsPartOfList_insertMissingAndUpdateExisting() =
        runBlocking {
            // GIVEN: `podcasts` table contains the podcast.
            val podcastOne = PodcastEntityPartialWithGenre(
                "one",
                "one",
                "one",
                "one",
                "one",
                false,
                100,
                100L,
                11
            )
            podcastDao.insertPartialWithGenre(podcastOne)

            // WHEN: Insert the podcasts into the `podcasts` table.
            val newOne = podcastOne.copy(title = "updated")
            val newTwo = podcastOne.copy("two", "two")
            val newThree = podcastOne.copy("three", "three")
            podcastDao.insertPartialWithGenre(listOf(newOne, newTwo, newThree))

            // THEN: All podcasts are valid.
            val loadedOne = podcastDao.getPodcast(podcastOne.id)
            val loadedTwo = podcastDao.getPodcast(newTwo.id)
            val loadedThree = podcastDao.getPodcast(newThree.id)
            assertThat(loadedOne?.title).isEqualTo(newOne.title)
            assertThat(loadedTwo?.title).isEqualTo(newTwo.title)
            assertThat(loadedThree?.title).isEqualTo(newThree.title)
        }
}