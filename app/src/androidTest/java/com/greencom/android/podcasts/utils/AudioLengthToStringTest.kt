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
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SmallTest
class AudioLengthToStringTest {

    private lateinit var context: Context

    private fun Int.hours() = this * TimeUnit.HOURS.toSeconds(1).toInt()
    private fun Int.minutes() = this * TimeUnit.MINUTES.toSeconds(1).toInt()
    private fun Int.seconds() = this

    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_1Hour23Minutes10Seconds() {
        val h = 1
        val m = 23
        val s = 10
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_full, h, m))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_1Hour23Minutes50Seconds() {
        val h = 1
        val m = 23
        val s = 50
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_full, h, m + 1))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_2Hours() {
        val h = 2
        val m = 0
        val s = 0
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_hours, h))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_2Hours10Seconds() {
        val h = 2
        val m = 0
        val s = 10
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_hours, h))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_2Hours50Seconds() {
        val h = 2
        val m = 0
        val s = 50
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_full, h, 1))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_10Minutes() {
        val h = 0
        val m = 10
        val s = 0
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_minutes, m))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_50Minutes() {
        val h = 0
        val m = 50
        val s = 0
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_minutes, m))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_40Minutes10Seconds() {
        val h = 0
        val m = 40
        val s = 10
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_minutes, m))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_40Minutes50Seconds() {
        val h = 0
        val m = 40
        val s = 50
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_minutes, m + 1))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_10Seconds() {
        val h = 0
        val m = 0
        val s = 10
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_minutes, 1))
    }

    @Test
    @Throws(Exception::class)
    fun audioLengthToString_50Seconds() {
        val h = 0
        val m = 0
        val s = 50
        val length = h.hours() + m.minutes() + s.seconds()

        val result = audioLengthToString(length, context)

        assertThat(result).isEqualTo(context.getString(R.string.podcast_length_minutes, 1))
    }
}