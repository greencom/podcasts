package com.greencom.android.podcasts.utils

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.greencom.android.podcasts.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@RunWith(AndroidJUnit4::class)
@SmallTest
class EpisodeDateToStringTest {

    private lateinit var context: Context
    private var currentTime = System.currentTimeMillis()

    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_10Minutes() {
        val timeFromCurrent = Duration.minutes(10)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_pub_just_now))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_50Minutes() {
        val timeFromCurrent = Duration.minutes(50)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_pub_just_now))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_1Hour() {
        val timeFromCurrent = Duration.hours(1)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        assertThat(result).isEqualTo(context.resources.getQuantityString(
            R.plurals.podcast_episode_pub_hours_ago, 1,1))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_10Hours30Minutes() {
        val timeFromCurrent = Duration.hours(10) + Duration.minutes(30)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        assertThat(result).isEqualTo(context.resources.getQuantityString(
            R.plurals.podcast_episode_pub_hours_ago, 10,10))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_23Hours() {
        val timeFromCurrent = Duration.hours(23)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        assertThat(result).isEqualTo(context.resources.getQuantityString(
            R.plurals.podcast_episode_pub_hours_ago, 23,23))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_1Day() {
        val timeFromCurrent = Duration.days(1)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        assertThat(result).isEqualTo(context.resources.getQuantityString(
            R.plurals.podcast_episode_pub_days_ago, 1,1))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_3Days10Hours() {
        val timeFromCurrent = Duration.days(3) + Duration.hours(10)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        assertThat(result).isEqualTo(context.resources.getQuantityString(
            R.plurals.podcast_episode_pub_days_ago, 3,3))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_7Days10Hours() {
        val timeFromCurrent = Duration.days(7) + Duration.hours(10)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        assertThat(result).isEqualTo(context.resources.getQuantityString(
            R.plurals.podcast_episode_pub_days_ago, 7,7))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_8Days() {
        val timeFromCurrent = Duration.days(8)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
        assertThat(result).isEqualTo(dateFormatter.format(date))
    }

    @Test
    @Throws(Exception::class)
    fun episodePubDateToString_30Days() {
        val timeFromCurrent = Duration.days(30)
        val date = currentTime - timeFromCurrent.inWholeMilliseconds

        val result = episodeDateToString(date, context)

        val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
        assertThat(result).isEqualTo(dateFormatter.format(date))
    }
}