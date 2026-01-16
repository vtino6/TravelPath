package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "etapes_medias",
    primaryKeys = ["etapeId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = Etape::class,
            parentColumns = ["id"],
            childColumns = ["etapeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Media::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["etapeId", "ordre"]),
        Index(value = ["mediaId"])
    ]
)
data class EtapeMedia(
    val etapeId: String,
    val mediaId: String,
    val ordre: Int = 0
)
