package com.greencom.android.podcasts.data.domain

/** Domain model class that represents the last search. */
data class LastSearch(
    val query: String,
    val result: PodcastSearchResult
)