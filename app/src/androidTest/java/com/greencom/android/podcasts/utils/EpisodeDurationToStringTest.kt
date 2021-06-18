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
class EpisodeDurationToStringTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_10Seconds() {
        val duration = Duration.seconds(10)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_m, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_50Seconds() {
        val duration = Duration.seconds(50)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_m, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_10Minutes10Seconds() {
        val duration = Duration.minutes(10) + Duration.seconds(10)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_m, 10))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_10Minutes50Seconds() {
        val duration = Duration.minutes(10) + Duration.seconds(50)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_m, 11))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_50Minutes10Seconds() {
        val duration = Duration.minutes(50) + Duration.seconds(10)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_m, 50))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_50Minutes50Seconds() {
        val duration = Duration.minutes(50) + Duration.seconds(50)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_m, 51))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_59Minutes50Seconds() {
        val duration = Duration.minutes(59) + Duration.seconds(50)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_h, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_1Hour() {
        val duration = Duration.hours(1)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_h, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_1Hour10Seconds() {
        val duration = Duration.hours(1) + Duration.seconds(10)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_h, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_1Hour50Seconds() {
        val duration = Duration.hours(1) + Duration.seconds(50)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_h_m, 1, 1))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_1Hour10Minutes() {
        val duration = Duration.hours(1) + Duration.minutes(10)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_h_m, 1, 10))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_1Hour10Minutes10Seconds() {
        val duration = Duration.hours(1) + Duration.minutes(10) + Duration.seconds(10)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_h_m, 1, 10))
    }

    @Test
    @Throws(Exception::class)
    fun episodeDurationToString_1Hour10Minutes50Seconds() {
        val duration = Duration.hours(1) + Duration.minutes(10) + Duration.seconds(50)

        val result = episodeDurationToString(duration, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_episode_duration_h_m, 1, 11))
    }
}