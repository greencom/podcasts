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
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@RunWith(AndroidJUnit4::class)
@SmallTest
class EpisodeTimeLeftToStringTest {

    private lateinit var context: Context
    private val duration = Duration.hours(2)

    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_10Seconds() {
        val timeLeft = Duration.seconds(10)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_almost_over))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_50Seconds() {
        val timeLeft = Duration.seconds(50)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_almost_over))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_10Minutes10Seconds() {
        val timeLeft = Duration.minutes(10) + Duration.seconds(10)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_m, 10))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_10Minutes50Seconds() {
        val timeLeft = Duration.minutes(10) + Duration.seconds(50)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_m, 11))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_50Minutes10Seconds() {
        val timeLeft = Duration.minutes(50) + Duration.seconds(10)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_m, 50))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_50Minutes50Seconds() {
        val timeLeft = Duration.minutes(50) + Duration.seconds(50)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_m, 51))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_59Minutes10Seconds() {
        val timeLeft = Duration.minutes(59) + Duration.seconds(10)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_m, 59))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_59Minutes50Seconds() {
        val timeLeft = Duration.minutes(59) + Duration.seconds(50)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_1Hour() {
        val timeLeft = Duration.hours(1)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_1Hour10Seconds() {
        val timeLeft = Duration.hours(1) + Duration.seconds(10)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_1Hour50Seconds() {
        val timeLeft = Duration.hours(1) + Duration.seconds(50)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h_m, 1, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_1Hour10Minutes() {
        val timeLeft = Duration.hours(1) + Duration.minutes(10)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h_m, 1, 10))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_1Hour10Minutes10Seconds() {
        val timeLeft = Duration.hours(1) + Duration.minutes(10) + Duration.seconds(10)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h_m, 1, 10))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_1Hour10Minutes50Seconds() {
        val timeLeft = Duration.hours(1) + Duration.minutes(10) + Duration.seconds(50)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h_m, 1, 11))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_1Hour59Minutes10Seconds() {
        val timeLeft = Duration.hours(1) + Duration.minutes(59) + Duration.seconds(10)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h_m, 1, 59))
    }

    @Test
    @Throws(Exception::class)
    fun episodeTimeLeftToString_1Hour59Minutes50Seconds() {
        val timeLeft = Duration.hours(1) + Duration.minutes(59) + Duration.seconds(50)
        val position = (duration - timeLeft).inWholeMilliseconds

        val result = episodeTimeLeftToString(position, duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_time_left_h, 2))
    }
}