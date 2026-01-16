package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medias",
    indices = [
        Index(value = ["typeMedia"]),
        Index(value = ["estEnCache"])
    ]
)
data class Media(
    @PrimaryKey
    val id: String,
    val typeMedia: TypeMedia,
    val url: String,
    val urlMiniature: String?,
    val estEnCache: Boolean = false,
    val cheminCache: String?,
    val dateCreation: Long = System.currentTimeMillis()
)
