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
    fun getGenre_genreExists_returnGenre() = runBlockingTest {
        // GIVEN - Insert a genre list.
        val genre1 = GenreEntity(1, "First", 0)
        val genre2 = GenreEntity(2, "Second", 0)
        val genres = listOf(genre1, genre2)
        genreDao.insert(genres)

        // WHEN - Get the genre by ID from the database.
        val loadedFirst = genreDao.getGenre(genre1.id)
        val loadedSecond = genreDao.getGenre(genre2.id)

        // THEN - The loaded genres are valid.
        assertThat(loadedFirst?.id).isEqualTo(genre1.id)
        assertThat(loadedFirst?.name).isEqualTo(genre1.name)

        assertThat(loadedSecond?.id).isEqualTo(genre2.id)
        assertThat(loadedSecond?.name).isEqualTo(genre2.name)
    }

    @Test
    fun getGenre_genreDoesNotExist_returnNull() = runBlockingTest {
        // GIVEN - Empty `genres` table.

        // WHEN - Get the non-existent genre.
        val loaded = genreDao.getGenre(1)

        // THEN - The loaded genre is null.
        assertThat(loaded).isNull()
    }

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