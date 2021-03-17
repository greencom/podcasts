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

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class GenreDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var genreDao: GenreDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        genreDao = database.genreDao()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun getSize_emptyTable_returnZero() = runBlockingTest {
        // GIVEN - Empty `genres` table.

        // WHEN - Get size of the table.
        val size = genreDao.getSize()

        // THEN - Size is 0.
        assertThat(size).isEqualTo(0)
    }

    @Test
    fun getSize_notEmptyTable_returnNotZero() = runBlockingTest {
        // GIVEN - Not empty `genres` table.
        val genres = listOf(
            GenreEntity(1, "First", 0),
            GenreEntity(2, "Second", 0)
        )
        genreDao.insert(genres)

        // WHEN - Get size of the table.
        val size = genreDao.getSize()

        // THEN - Size is not 0.
        assertThat(size).isEqualTo(genres.size)
    }
}