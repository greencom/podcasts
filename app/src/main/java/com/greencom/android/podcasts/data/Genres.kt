package com.greencom.android.podcasts.data

import com.squareup.moshi.Json

data class Genre(
        val id: Int,
        val name: String,
        @Json(name = "parent_id") val parentId: Int?,
)

data class Genres(val genres: List<Genre>)
