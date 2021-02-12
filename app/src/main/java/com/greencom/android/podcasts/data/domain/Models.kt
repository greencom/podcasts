package com.greencom.android.podcasts.data.domain

/** Model class that represents a domain genre object. */
data class Genre(

    /** Genre ID. */
    val id: Int,

    /** Genre name. */
    val name: String,

    /**
     * Parent genre ID.
     *
     * If the property has the [Genre.NO_PARENT_GENRE] value, it means
     * that this genre does not have a parent genre.
     */
    val parentId: Int,
) {
    companion object {

        /** Constant that means the genre does not have a parent genre. */
        const val NO_PARENT_GENRE = -1
    }
}
