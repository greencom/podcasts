package com.greencom.android.podcasts.data

import com.squareup.moshi.Json

/** Model class for `ListenApiService.getGenres()` response. */
data class Genres(val genres: List<Genre>)

data class Genre(
        /** Genre ID. */
        val id: Int,
        /** Genre name. */
        val name: String,
        /** Parent genre ID. */
        @Json(name = "parent_id") val parentId: Int?,
)
