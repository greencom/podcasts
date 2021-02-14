package com.greencom.android.podcasts.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greencom.android.podcasts.data.domain.Genre
import com.greencom.android.podcasts.data.domain.Podcast

/** Model class that represents a podcast entity in the database. */
@Entity(tableName = "podcasts")
data class PodcastEntity(

    /** Podcast id. */
    @PrimaryKey
    val id: String,

    /** Podcast title. */
    val title: String,

    /** Podcast description. */
    val description: String,

    /** Image URL. */
    val image: String,

    /** Podcast publisher. */
    val publisher: String,

    /** Whether this podcast contains explicit language. */
    val explicitContent: Boolean,

    /** Total number of episodes in this podcast. */
    val episodeCount: Int,

    /** Podcast genre ids. */
    val genreIds: List<Int>,

    /**
     * The genre ID for which the podcast is featured on the best list.
     * If the value is [Podcast.NOT_IN_BEST], it means that the podcast
     * is not on the best list for any genre.
     */
    val inBestForGenre: Int,

    /**
     * The estimated popularity score of a podcast compared to all other
     * podcasts in the world on a scale from 0 to 100.
     */
    val score: Int,
)



/** Model class that represents a episode entity in the database. */
@Entity(tableName = "episodes")
data class EpisodeEntity(

    /** Episode id. */
    @PrimaryKey
    val id: String,

    /** Episode title. */
    val title: String,

    /** Episode description. */
    val description: String,

    /** Image URL. */
    val image: String,

    /** Audio URL. */
    val audio: String,

    /** Audio length in seconds. */
    val audioLength: Int,

    /** The ID of the podcast that this episode belongs to. */
    val podcastId: String,

    /** Whether this podcast contains explicit language. */
    val explicitContent: Boolean,

    /** Published date in milliseconds. */
    val date: Long,
)



/** Model class that represents a genre entity in the database. */
@Entity(tableName = "genres")
data class GenreEntity(

    /** Genre ID. */
    @PrimaryKey
    val id: Int,

    /** Genre name. */
    val name: String,

    /**
     * Parent genre ID.
     *
     * If the value is [Genre.NO_PARENT_GENRE], it means
     * that this genre does not have a parent genre.
     */
    val parentId: Int,
)

/** Convert [GenreEntity] list to a list of [Genre]s. */
fun List<GenreEntity>.asDomainModel(): List<Genre> {
    return map {
        Genre(
            id = it.id,
            name = it.name,
            parentId = it.parentId,
        )
    }
}
