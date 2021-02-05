package com.greencom.android.podcasts.data.domain

/** Model class that represents a domain genre object. */
data class Genre(

        /** Genre ID. */
        val id: Int,

        /** Genre name. */
        val name: String,

        /** Parent genre ID. `-1` means that there is no parent genre. */
        val parentId: Int,
)