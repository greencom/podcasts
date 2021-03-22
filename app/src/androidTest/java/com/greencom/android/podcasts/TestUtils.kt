package com.greencom.android.podcasts

import com.greencom.android.podcasts.data.database.PodcastEntity
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Create a test [PodcastEntity] object with a given ID.
 *
 * - `title`, `description`, `image` and `publisher` string properties are filled
 *   according to the `"*property*_*id*"` scheme.
 * - `explicitContent`, `episodeCount` and `latestPubDate` properties are random.
 * - `genreId` is `1`.
 */
fun createTestPodcastEntity(id: String): PodcastEntity = PodcastEntity(
    id = id,
    title = "title_$id",
    description = "description_$id",
    image = "image_$id",
    publisher = "publisher_$id",
    explicitContent = Math.random() >= 0.5,
    episodeCount = (Math.random() * 500).roundToInt(),
    latestPubDate = (Math.random() * 10000).roundToLong(),
    genreId = 1
)