package com.greencom.android.podcasts.network

import com.squareup.moshi.Json

/** Wrapper class for a [ListenApiService.searchEpisode] response. */
data class SearchEpisodeWrapper(

    /** The number of search results on this page. */
    @Json(name = "count")
    val count: Int,

    /** The total number of search results. */
    @Json(name = "total")
    val total: Int,

    /** A list of search results. */
    @Json(name = "results")
    val episodes: List<SearchEpisodeItem>,

    /**
     * Pass this value to the `offset` parameter of `searchEpisode()` to do
     * pagination of search results.
     */
    @Json(name = "next_offset")
    val nextOffset: Int,
) {

    /** Wrapper class for a single episode object in the [SearchEpisodeWrapper.episodes] list. */
    data class SearchEpisodeItem(

        /** Episode ID. */
        @Json(name = "id")
        val id: String,

        /** Episode title. */
        @Json(name = "title_original")
        val title: String,

        /** Episode description. */
        @Json(name = "description_original")
        val description: String,

        /** Image URL for this episode. */
        @Json(name = "image")
        val image: String,

        /** Audio URL for this episode. */
        @Json(name = "audio")
        val audio: String,

        /** Audio length of this episode in seconds. */
        @Json(name = "audio_length_sec")
        val audioLength: Int,

        /** The podcast that this episode belongs to. */
        @Json(name = "podcast")
        val podcast: SearchEpisodeItemPodcast,

        /** Whether this podcast contains explicit language. */
        @Json(name = "explicit_content")
        val explicitContent: Boolean,

        /** Published date in milliseconds. */
        @Json(name = "pub_date_ms")
        val date: Long,
    ) {

        /** Wrapper class for a [SearchEpisodeWrapper.SearchEpisodeItem.podcast] object. */
        data class SearchEpisodeItemPodcast(

            /** Podcast ID. */
            @Json(name = "id")
            val id: String,

            /** Podcast name. */
            @Json(name = "title_original")
            val title: String,

            /** Image URL. */
            @Json(name = "image")
            val image: String,

            /** Podcast publisher. */
            @Json(name = "publisher_original")
            val publisher: String,
        )
    }
}

/** Wrapper class for a [ListenApiService.searchPodcast] response. */
data class SearchPodcastWrapper(

    /** The number of search results in this page. */
    @Json(name = "count")
    val count: Int,

    /** The total number of search results. */
    @Json(name = "total")
    val total: Int,

    /** A list of search results. */
    @Json(name = "results")
    val podcasts: List<SearchPodcastItem>,

    /**
     * Pass this value to the `offset` parameter of `searchPodcast()` to do
     * pagination of search results.
     */
    @Json(name = "next_offset")
    val nextOffset: Int,
) {

    /** Wrapper class for a single podcast object in the [SearchPodcastWrapper.podcasts] list. */
    data class SearchPodcastItem(

        /** Podcast ID. */
        @Json(name = "id")
        val id: String,

        /** Podcast name. */
        @Json(name = "title_original")
        val title: String,

        /** Podcast description. */
        @Json(name = "description_original")
        val description: String,

        /** Image URL for this podcast. */
        @Json(name = "image")
        val image: String,

        /** Podcast publisher. */
        @Json(name = "publisher_original")
        val publisher: String,

        /** Whether this podcast contains explicit language. */
        @Json(name = "explicit_content")
        val explicitContent: Boolean,

        /** Total number of episodes in this podcast. */
        @Json(name = "total_episodes")
        val episodeCount: Int,

        /** The published date of the latest episode of this podcast in milliseconds. */
        @Json(name = "latest_pub_date_ms")
        val latestPubDate: Long,

        /** The published date of the oldest episode of this podcast in milliseconds. */
        @Json(name = "earliest_pub_date_ms")
        val earliestPubDate: Long,
    )
}

/** Wrapper class for a [ListenApiService.getPodcast] response. */
data class PodcastWrapper(

    /** Podcast ID. */
    @Json(name = "id")
    val id: String,

    /** Podcast name. */
    @Json(name = "title")
    val title: String,

    /** Podcast description. */
    @Json(name = "description")
    val description: String,

    /** Image URL for this podcast. */
    @Json(name = "image")
    val image: String,

    /** Podcast publisher. */
    @Json(name = "publisher")
    val publisher: String,

    /** Whether this podcast contains explicit language. */
    @Json(name = "explicit_content")
    val explicitContent: Boolean,

    /** Total number of episodes in this podcast. */
    @Json(name = "total_episodes")
    val episodeCount: Int,

    /** The published date of the latest episode of this podcast in milliseconds. */
    @Json(name = "latest_pub_date_ms")
    val latestPubDate: Long,

    /** The published date of the oldest episode of this podcast in milliseconds. */
    @Json(name = "earliest_pub_date_ms")
    val earliestPubDate: Long,

    /** Episodes of this podcast. */
    @Json(name = "episodes")
    val episodes: List<PodcastItem>,

    /**
     * Pass it to the `nextEpisodePubDate` parameter of [ListenApiService.getPodcast]
     * to paginate through episodes of that podcast.
     *
     * Note: Nullable because of ListenAPI bug(?) that may return null.
     */
    @Json(name = "next_episode_pub_date")
    val nextEpisodePubDate: Long?,
) {

    /** Wrapper class for a single episode object in the [PodcastWrapper.episodes] list. */
    data class PodcastItem(

        /** Episode ID. */
        @Json(name = "id")
        val id: String,

        /** Episode title. */
        @Json(name = "title")
        val title: String,

        /** Episode description. */
        @Json(name = "description")
        val description: String,

        /** Image URL for this episode. */
        @Json(name = "image")
        val image: String,

        /** Audio URL for this episode. */
        @Json(name = "audio")
        val audio: String,

        /** Audio length of this episode in seconds. */
        @Json(name = "audio_length_sec")
        val audioLength: Int,

        /** Whether this podcast contains explicit language. */
        @Json(name = "explicit_content")
        val explicitContent: Boolean,

        /** Published date in milliseconds. */
        @Json(name = "pub_date_ms")
        val date: Long,
    )
}

/** Wrapper class for a [ListenApiService.getBestPodcasts] response. */
data class BestPodcastsWrapper(

    /** A list of search results. */
    @Json(name = "podcasts")
    val podcasts: List<BestPodcastsItem>,

    /** Genre ID for which the best podcasts list is made for. */
    @Json(name = "id")
    val genreId: Int,

    /** Genre name. */
    @Json(name = "name")
    val genreName: String,

    /** Whether there is the next page of response. */
    @Json(name = "has_next")
    val hasNextPage: Boolean,
) {

    /** Wrapper class for a single podcast object in the [BestPodcastsWrapper.podcasts] list. */
    data class BestPodcastsItem(

        /** Podcast ID. */
        @Json(name = "id")
        val id: String,

        /** Podcast name. */
        @Json(name = "title")
        val title: String,

        /** Podcast description. */
        @Json(name = "description")
        val description: String,

        /** Image URL for this podcast. */
        @Json(name = "image")
        val image: String,

        /** Podcast publisher. */
        @Json(name = "publisher")
        val publisher: String,

        /** Whether this podcast contains explicit language. */
        @Json(name = "explicit_content")
        val explicitContent: Boolean,

        /** Total number of episodes in this podcast. */
        @Json(name = "total_episodes")
        val episodeCount: Int,

        /** The published date of the latest episode of this podcast in milliseconds. */
        @Json(name = "latest_pub_date_ms")
        val latestPubDate: Long,

        /** The published date of the oldest episode of this podcast in milliseconds. */
        @Json(name = "earliest_pub_date_ms")
        val earliestPubDate: Long,
    )
}