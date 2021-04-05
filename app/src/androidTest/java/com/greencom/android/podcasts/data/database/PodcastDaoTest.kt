package com.greencom.android.podcasts.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

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
    @Throws(IOException::class)
    fun closeDatabase() = database.close()

    // Tests are commented out since methods used by them marked as protected.
    // Make them public to run tests.

//    @Test
//    @Throws(Exception::class)
//    fun getPodcast_emptyTable_returnNull() = runBlockingTest {
//        // GIVEN: Empty `podcasts` table.
//
//        // WHEN: Get a podcast.
//        val loaded = podcastDao.getPodcast("id")
//
//        // THEN: loaded is null.
//        assertThat(loaded).isNull()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getPodcast_podcastExists_returnPodcast() = runBlockingTest {
//        // GIVEN: `podcasts` table contains a podcast.
//        val podcast = PodcastEntityPartial(
//            "id",
//            "title",
//            "desc",
//            "image",
//            "publisher",
//            false,
//            100,
//            100L
//        )
//        podcastDao.insertPartialToTemp(podcast)
//        podcastDao.merge()
//
//        // WHEN: Get a podcast.
//        val loaded = podcastDao.getPodcast(podcast.id)
//
//        // THEN: loaded is valid.
//        assertThat(loaded?.image).isEqualTo(podcast.image)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getPodcastFromTemp_emptyTable_returnNull() = runBlockingTest {
//        // GIVEN: Empty `podcasts_temp` table.
//
//        // WHEN: Get a podcast.
//        val loaded = podcastDao.getPodcastFromTemp("id")
//
//        // THEN: loaded is null.
//        assertThat(loaded).isNull()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getPodcastFromTemp_podcastExists_returnPodcast() = runBlockingTest {
//        // GIVEN: `podcasts_temp` table contains a podcast.
//        val podcast = PodcastEntityPartial(
//            "id",
//            "title",
//            "desc",
//            "image",
//            "publisher",
//            false,
//            100,
//            100L
//        )
//        podcastDao.insertPartialToTemp(podcast)
//
//        // WHEN: Get a podcast.
//        val loaded = podcastDao.getPodcastFromTemp(podcast.id)
//
//        // THEN: loaded is valid.
//        assertThat(loaded?.image).isEqualTo(podcast.image)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun insertPartialToTemp_insertSinglePodcast_returnPodcast() = runBlockingTest {
//        // GIVEN: Insert a partial podcast into the `podcasts_temp` table.
//        val podcast = PodcastEntityPartial(
//            "id",
//            "title",
//            "desc",
//            "image",
//            "publisher",
//            false,
//            100,
//            100L
//        )
//
//        // WHEN: Insert a podcast and get it back by ID.
//        podcastDao.insertPartialToTemp(podcast)
//        val loaded = podcastDao.getPodcastFromTemp(podcast.id)
//
//        // THEN: The podcast is not null, `genreId` = -1, `subscribed` = false.
//        assertThat(loaded?.title).isEqualTo(podcast.title)
//        assertThat(loaded?.genreId).isEqualTo(-1)
//        assertThat(loaded?.subscribed).isFalse()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun insertPartialToTemp_insertListOfPodcasts_returnPodcasts() = runBlockingTest {
//        // GIVEN: Insert a list of partial podcasts into the `podcasts_temp` table.
//        val podcastOne = PodcastEntityPartial(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L
//        )
//        val podcastTwo = podcastOne.copy("two", "two")
//        val podcastThree = podcastOne.copy("three", "three")
//
//        // WHEN: Insert a list of podcasts and get them back by ID.
//        podcastDao.insertPartialToTemp(listOf(podcastOne, podcastTwo, podcastThree))
//        val loadedOne = podcastDao.getPodcastFromTemp(podcastOne.id)
//        val loadedTwo = podcastDao.getPodcastFromTemp(podcastTwo.id)
//        val loadedThree = podcastDao.getPodcastFromTemp(podcastThree.id)
//
//        // THEN: The podcasts are not null, `genreId` = -1, `subscribed` = false for each.
//        assertThat(loadedOne?.title).isEqualTo(podcastOne.title)
//        assertThat(loadedOne?.genreId).isEqualTo(-1)
//        assertThat(loadedOne?.subscribed).isFalse()
//
//        assertThat(loadedTwo?.title).isEqualTo(podcastTwo.title)
//        assertThat(loadedTwo?.genreId).isEqualTo(-1)
//        assertThat(loadedTwo?.subscribed).isFalse()
//
//        assertThat(loadedThree?.title).isEqualTo(podcastThree.title)
//        assertThat(loadedThree?.genreId).isEqualTo(-1)
//        assertThat(loadedThree?.subscribed).isFalse()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun insertPartialWithGenreToTemp_insertSinglePodcast_returnPodcast() = runBlockingTest {
//        // GIVEN: Insert a partial podcast into the `podcasts_temp` table.
//        val podcast = PodcastEntityPartialWithGenre(
//            "id",
//            "title",
//            "desc",
//            "image",
//            "publisher",
//            false,
//            100,
//            100L,
//            5
//        )
//
//        // WHEN: Insert a podcast and get it back by ID.
//        podcastDao.insertPartialWithGenreToTemp(podcast)
//        val loaded = podcastDao.getPodcastFromTemp(podcast.id)
//
//        // THEN: The podcast is not null, `genreId` = 5, `subscribed` = false.
//        assertThat(loaded?.title).isEqualTo(podcast.title)
//        assertThat(loaded?.genreId).isEqualTo(podcast.genreId)
//        assertThat(loaded?.subscribed).isFalse()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun insertPartialWithGenreToTemp_insertListOfPodcasts_returnPodcasts() = runBlockingTest {
//        // GIVEN: Insert a list of partial podcasts into the `podcasts_temp` table.
//        val podcastOne = PodcastEntityPartialWithGenre(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L,
//            5
//        )
//        val podcastTwo = podcastOne.copy("two", "two", genreId = 2)
//        val podcastThree = podcastOne.copy("three", "three", genreId = 44)
//
//        // WHEN: Insert a list of podcasts and get them back by ID.
//        podcastDao.insertPartialWithGenreToTemp(listOf(podcastOne, podcastTwo, podcastThree))
//        val loadedOne = podcastDao.getPodcastFromTemp(podcastOne.id)
//        val loadedTwo = podcastDao.getPodcastFromTemp(podcastTwo.id)
//        val loadedThree = podcastDao.getPodcastFromTemp(podcastThree.id)
//
//        // THEN: The podcasts are not null, `genreId` is valid, `subscribed` = false for each.
//        assertThat(loadedOne?.title).isEqualTo(podcastOne.title)
//        assertThat(loadedOne?.genreId).isEqualTo(podcastOne.genreId)
//        assertThat(loadedOne?.subscribed).isFalse()
//
//        assertThat(loadedTwo?.title).isEqualTo(podcastTwo.title)
//        assertThat(loadedTwo?.genreId).isEqualTo(podcastTwo.genreId)
//        assertThat(loadedTwo?.subscribed).isFalse()
//
//        assertThat(loadedThree?.title).isEqualTo(podcastThree.title)
//        assertThat(loadedThree?.genreId).isEqualTo(podcastThree.genreId)
//        assertThat(loadedThree?.subscribed).isFalse()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun merge_podcastsTableMissingAllPodcasts_allPodcastsInsertedIntoPodcastsTable() =
//        runBlockingTest {
//            // GIVEN: `podcasts` table does not contain any of new podcasts. New podcasts inserted
//            // into the `podcasts_temp` table.
//            val podcastOne = PodcastEntityPartial(
//                "one",
//                "one",
//                "one",
//                "one",
//                "one",
//                false,
//                100,
//                100L
//            )
//            val podcastTwo = podcastOne.copy("two", "two")
//            val podcastThree = podcastOne.copy("three", "three")
//
//            // WHEN: Insert, merge and get three podcasts by ID from the `podcasts` table.
//            podcastDao.insertPartialToTemp(listOf(podcastOne, podcastTwo, podcastThree))
//            podcastDao.merge()
//            val loadedOne = podcastDao.getPodcast(podcastOne.id)
//            val loadedTwo = podcastDao.getPodcast(podcastTwo.id)
//            val loadedThree = podcastDao.getPodcast(podcastThree.id)
//
//            // THEN: The podcasts are not null, `genreId` = -1, `subscribed` = false for each.
//            assertThat(loadedOne?.title).isEqualTo(podcastOne.title)
//            assertThat(loadedOne?.genreId).isEqualTo(-1)
//            assertThat(loadedOne?.subscribed).isFalse()
//
//            assertThat(loadedTwo?.title).isEqualTo(podcastTwo.title)
//            assertThat(loadedTwo?.genreId).isEqualTo(-1)
//            assertThat(loadedTwo?.subscribed).isFalse()
//
//            assertThat(loadedThree?.title).isEqualTo(podcastThree.title)
//            assertThat(loadedThree?.genreId).isEqualTo(-1)
//            assertThat(loadedThree?.subscribed).isFalse()
//        }
//
//    @Test
//    @Throws(Exception::class)
//    fun merge_podcastsTableContainsOneOfThreePodcasts_missingPodcastsInsertedIntoPodcastsTable() =
//        runBlockingTest {
//            // GIVEN: `podcasts` contains one podcast already. New podcasts inserted
//            // into the `podcasts_temp` table.
//            val podcastOne = PodcastEntityPartial(
//                "one",
//                "one",
//                "one",
//                "one",
//                "one",
//                false,
//                100,
//                100L
//            )
//            val podcastTwo = podcastOne.copy("two", "two")
//            val podcastThree = podcastOne.copy("three", "three")
//            podcastDao.insertPartialToTemp(podcastOne)
//            podcastDao.merge()
//            podcastDao.clearTemp()
//
//            // WHEN: Insert, merge and get three podcasts by ID from the `podcasts` table.
//            podcastDao.insertPartialToTemp(listOf(podcastOne, podcastTwo, podcastThree))
//            podcastDao.merge()
//            val loadedOne = podcastDao.getPodcast(podcastOne.id)
//            val loadedTwo = podcastDao.getPodcast(podcastTwo.id)
//            val loadedThree = podcastDao.getPodcast(podcastThree.id)
//
//            // THEN: The podcasts are not null, `genreId` = -1, `subscribed` = false for each.
//            assertThat(loadedOne?.title).isEqualTo(podcastOne.title)
//            assertThat(loadedOne?.genreId).isEqualTo(-1)
//            assertThat(loadedOne?.subscribed).isFalse()
//
//            assertThat(loadedTwo?.title).isEqualTo(podcastTwo.title)
//            assertThat(loadedTwo?.genreId).isEqualTo(-1)
//            assertThat(loadedTwo?.subscribed).isFalse()
//
//            assertThat(loadedThree?.title).isEqualTo(podcastThree.title)
//            assertThat(loadedThree?.genreId).isEqualTo(-1)
//            assertThat(loadedThree?.subscribed).isFalse()
//        }
//
//    @Test
//    @Throws(Exception::class)
//    fun merge_podcastsTableContainsAllPodcasts_podcastsTableIsNotChanged() = runBlockingTest {
//        // GIVEN: `podcasts` contains one podcast already. New podcasts inserted
//        // into the `podcasts_temp` table.
//        val podcastOne = PodcastEntityPartial(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L
//        )
//        val podcastTwo = podcastOne.copy("two", "two")
//        val podcastThree = podcastOne.copy("three", "three")
//        podcastDao.insertPartialToTemp(listOf(podcastOne, podcastTwo, podcastThree))
//        podcastDao.merge()
//        podcastDao.clearTemp()
//
//        // WHEN: Insert already existing podcasts with updated fields, merge and get
//        // three podcasts by ID from the `podcasts` table.
//        val newOne = podcastOne.copy(title = "updated", episodeCount = 200)
//        val newTwo = podcastTwo.copy(title = "updated", episodeCount = 200)
//        val newThree = podcastThree.copy(title = "updated", episodeCount = 200)
//        podcastDao.insertPartialToTemp(listOf(newOne, newTwo, newThree))
//        podcastDao.merge()
//        val loadedOne = podcastDao.getPodcast(podcastOne.id)
//        val loadedTwo = podcastDao.getPodcast(podcastTwo.id)
//        val loadedThree = podcastDao.getPodcast(podcastThree.id)
//
//        // THEN: The podcasts are not null, fields are not changed.
//        assertThat(loadedOne?.title).isEqualTo(podcastOne.title)
//        assertThat(loadedOne?.episodeCount).isEqualTo(podcastOne.episodeCount)
//        assertThat(loadedOne?.genreId).isEqualTo(-1)
//        assertThat(loadedOne?.subscribed).isFalse()
//
//        assertThat(loadedTwo?.title).isEqualTo(podcastTwo.title)
//        assertThat(loadedTwo?.episodeCount).isEqualTo(podcastTwo.episodeCount)
//        assertThat(loadedTwo?.genreId).isEqualTo(-1)
//        assertThat(loadedTwo?.subscribed).isFalse()
//
//        assertThat(loadedThree?.title).isEqualTo(podcastThree.title)
//        assertThat(loadedThree?.episodeCount).isEqualTo(podcastThree.episodeCount)
//        assertThat(loadedThree?.genreId).isEqualTo(-1)
//        assertThat(loadedThree?.subscribed).isFalse()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun update_updateExistingEntry_returnUpdated() = runBlockingTest {
//        // GIVEN: `podcasts` contains one podcast already.
//        val podcast = PodcastEntityPartial(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L
//        )
//        podcastDao.insertPartialToTemp(podcast)
//        podcastDao.merge()
//        podcastDao.clearTemp()
//
//        // WHEN: Update existing podcast with the new one and return updated.
//        val new = PodcastEntity(
//            podcast.id,
//            podcast.title,
//            "updated",
//            podcast.image,
//            podcast.publisher,
//            podcast.explicitContent,
//            200,
//            podcast.latestPubDate,
//            true,
//            genreId = -1
//        )
//        podcastDao.update(new)
//        val loaded = podcastDao.getPodcast(podcast.id)
//
//        // THEN: loaded is updated.
//        assertThat(loaded?.description).isEqualTo(new.description)
//        assertThat(loaded?.subscribed).isTrue()
//        assertThat(loaded?.episodeCount).isEqualTo(new.episodeCount)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun updatePartial_updateExistingEntry_returnUpdated() = runBlockingTest {
//        // GIVEN: `podcasts` contains one podcast already.
//        val podcast = PodcastEntityPartial(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L
//        )
//        podcastDao.insertPartialToTemp(podcast)
//        podcastDao.merge()
//        podcastDao.clearTemp()
//
//        // WHEN: Update existing podcast with the new one and return updated.
//        val new = PodcastEntityPartial(
//            podcast.id,
//            podcast.title,
//            "updated",
//            podcast.image,
//            podcast.publisher,
//            podcast.explicitContent,
//            200,
//            podcast.latestPubDate,
//        )
//        podcastDao.updatePartial(new)
//        val loaded = podcastDao.getPodcast(podcast.id)
//
//        // THEN: loaded is updated.
//        assertThat(loaded?.description).isEqualTo(new.description)
//        assertThat(loaded?.episodeCount).isEqualTo(new.episodeCount)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun updatePartial_updateExistingEntries_returnUpdated() = runBlockingTest {
//        // GIVEN: `podcasts` contains a list of podcasts already.
//        val podcastOne = PodcastEntityPartial(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L
//        )
//        val podcastTwo = podcastOne.copy("two")
//        podcastDao.insertPartialToTemp(listOf(podcastOne, podcastTwo))
//        podcastDao.merge()
//        podcastDao.clearTemp()
//
//        // WHEN: Update existing podcasts with the new ones and return updated.
//        val newOne = PodcastEntityPartial(
//            podcastOne.id,
//            podcastOne.title,
//            "updated",
//            podcastOne.image,
//            podcastOne.publisher,
//            podcastOne.explicitContent,
//            200,
//            podcastOne.latestPubDate,
//        )
//        val newTwo = PodcastEntityPartial(
//            podcastTwo.id,
//            podcastTwo.title,
//            "updated",
//            podcastTwo.image,
//            podcastTwo.publisher,
//            podcastTwo.explicitContent,
//            200,
//            podcastTwo.latestPubDate,
//        )
//        podcastDao.updatePartial(listOf(newOne, newTwo))
//        val loadedOne = podcastDao.getPodcast(podcastOne.id)
//        val loadedTwo = podcastDao.getPodcast(podcastTwo.id)
//
//        // THEN: returned podcasts are updated.
//        assertThat(loadedOne?.description).isEqualTo(newOne.description)
//        assertThat(loadedOne?.episodeCount).isEqualTo(newOne.episodeCount)
//        assertThat(loadedTwo?.description).isEqualTo(newTwo.description)
//        assertThat(loadedTwo?.episodeCount).isEqualTo(newTwo.episodeCount)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun updatePartialWithGenre_updateExistingEntry_returnUpdated() = runBlockingTest {
//        // GIVEN: `podcasts` contains one podcast already.
//        val podcast = PodcastEntityPartial(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L
//        )
//        podcastDao.insertPartialToTemp(podcast)
//        podcastDao.merge()
//        podcastDao.clearTemp()
//
//        // WHEN: Update existing podcast with the new one and return updated.
//        val new = PodcastEntityPartialWithGenre(
//            podcast.id,
//            podcast.title,
//            "updated",
//            podcast.image,
//            podcast.publisher,
//            podcast.explicitContent,
//            200,
//            podcast.latestPubDate,
//            genreId = 5
//        )
//        podcastDao.updatePartialWithGenre(new)
//        val loaded = podcastDao.getPodcast(podcast.id)
//
//        // THEN: loaded is updated.
//        assertThat(loaded?.description).isEqualTo(new.description)
//        assertThat(loaded?.episodeCount).isEqualTo(new.episodeCount)
//        assertThat(loaded?.genreId).isEqualTo(new.genreId)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun updatePartialWithGenre_updateExistingEntries_returnUpdated() = runBlockingTest {
//        // GIVEN: `podcasts` contains a list of podcasts already.
//        val podcastOne = PodcastEntityPartial(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L
//        )
//        val podcastTwo = podcastOne.copy("two")
//        podcastDao.insertPartialToTemp(listOf(podcastOne, podcastTwo))
//        podcastDao.merge()
//        podcastDao.clearTemp()
//
//        // WHEN: Update existing podcasts with the new ones and return updated.
//        val newOne = PodcastEntityPartialWithGenre(
//            podcastOne.id,
//            podcastOne.title,
//            "updated",
//            podcastOne.image,
//            podcastOne.publisher,
//            podcastOne.explicitContent,
//            200,
//            podcastOne.latestPubDate,
//            genreId = 15
//        )
//        val newTwo = PodcastEntityPartialWithGenre(
//            podcastTwo.id,
//            podcastTwo.title,
//            "updated",
//            podcastTwo.image,
//            podcastTwo.publisher,
//            podcastTwo.explicitContent,
//            200,
//            podcastTwo.latestPubDate,
//            genreId = 56
//        )
//        podcastDao.updatePartialWithGenre(listOf(newOne, newTwo))
//        val loadedOne = podcastDao.getPodcast(podcastOne.id)
//        val loadedTwo = podcastDao.getPodcast(podcastTwo.id)
//
//        // THEN: returned podcasts are updated.
//        assertThat(loadedOne?.description).isEqualTo(newOne.description)
//        assertThat(loadedOne?.episodeCount).isEqualTo(newOne.episodeCount)
//        assertThat(loadedOne?.genreId).isEqualTo(newOne.genreId)
//
//        assertThat(loadedTwo?.description).isEqualTo(newTwo.description)
//        assertThat(loadedTwo?.episodeCount).isEqualTo(newTwo.episodeCount)
//        assertThat(loadedTwo?.genreId).isEqualTo(newTwo.genreId)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun clearTemp_podcastsTempTableContainsPodcast_returnNull() = runBlockingTest {
//        // GIVEN: `podcasts_temp` contains a podcast.
//        val podcast = PodcastEntityPartial(
//            "one",
//            "one",
//            "one",
//            "one",
//            "one",
//            false,
//            100,
//            100L
//        )
//        podcastDao.insertPartialToTemp(podcast)
//
//        // WHEN: clear `podcasts_temp` and return the podcast.
//        podcastDao.clearTemp()
//        val loaded = podcastDao.getPodcastFromTemp(podcast.id)
//
//        // THEN: loaded is null.
//        assertThat(loaded).isNull()
//    }
}