package com.greencom.android.podcasts.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greencom.android.podcasts.data.domain.Genre

/** Model class that represents a genre entity in the database. */
@Entity(tableName = "genres")
data class GenreEntity(

        /** Genre ID. */
        @PrimaryKey
        val id: Int,

        /** Genre name. */
        val name: String,

        /** Parent genre ID. `-1` means that there is no parent genre. */
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