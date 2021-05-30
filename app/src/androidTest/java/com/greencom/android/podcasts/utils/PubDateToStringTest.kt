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
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@SmallTest
class PubDateToStringTest {

    private lateinit var context: Context
    private var currentTime = System.currentTimeMillis()

    @Before
    fun before() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_10Minutes() {
        val date = currentTime - TimeUnit.MINUTES.toMillis(10)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(context.getString(R.string.episode_pub_just_now))
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_30Minutes() {
        val date = currentTime - TimeUnit.MINUTES.toMillis(30)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(context.getString(R.string.episode_pub_just_now))
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_59Minutes30Seconds() {
        val date = currentTime -
                TimeUnit.MINUTES.toMillis(59) -
                TimeUnit.SECONDS.toMillis(30)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(context.getString(R.string.episode_pub_just_now))
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_1Hour() {
        val date = currentTime - TimeUnit.HOURS.toMillis(1)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.episode_pub_hours_ago,
                1,
                1
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_6Hours() {
        val date = currentTime - TimeUnit.HOURS.toMillis(6)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.episode_pub_hours_ago,
                6,
                6
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_23Hours30Minutes() {
        val date = currentTime -
                TimeUnit.HOURS.toMillis(23) -
                TimeUnit.MINUTES.toMillis(30)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.episode_pub_hours_ago,
                23,
                23
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_1Day() {
        val date = currentTime - TimeUnit.DAYS.toMillis(1)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.episode_pub_days_ago,
                1,
                1
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_3Days23Hours() {
        val date = currentTime -
                TimeUnit.DAYS.toMillis(3) -
                TimeUnit.HOURS.toMillis(23)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.episode_pub_days_ago,
                3,
                3
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_6Days23Hours59Minutes() {
        val date = currentTime -
                TimeUnit.DAYS.toMillis(6) -
                TimeUnit.HOURS.toMillis(23) -
                TimeUnit.MINUTES.toMillis(59)

        val result = pubDateToString(date, context)

        assertThat(result).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.episode_pub_days_ago,
                6,
                6
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_7Days() {
        val date = currentTime - TimeUnit.DAYS.toMillis(7)

        val result = pubDateToString(date, context)

        val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
        assertThat(result).isEqualTo(dateFormatter.format(date))
    }

    @Test
    @Throws(Exception::class)
    fun pubDateToString_45Days() {
        val date = currentTime - TimeUnit.DAYS.toMillis(45)

        val result = pubDateToString(date, context)

        val dateFormatter = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM)
        assertThat(result).isEqualTo(dateFormatter.format(date))
    }
}